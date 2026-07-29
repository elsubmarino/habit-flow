package io.streak.habitflow.global.aop;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;


@Aspect
@Component
@RequiredArgsConstructor
public class CheckOwnershipAspect {
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final LabelRepository labelRepository;
    private final HashidsProvider hashidsProvider;

    @Before("@annotation(CheckOwnership) || @annotation(CheckOwnerships)")
    public void validateOwnership(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        CheckOwnership[] checkOwnerships = method.getAnnotationsByType(CheckOwnership.class);

        String[]parameterNames = methodSignature.getParameterNames();
        Object[]args=joinPoint.getArgs();

        UUID publicTaskId = null;
        UUID publicCommentId = null;
        Long loginMemberId = null;
        UUID publicNotificationId = null;
        UUID publicProjectId = null;
        UUID publicLabelId = null;
        UUID publicMemberId = null;
        TaskRequest.Create request = null;

        for(int i=0;i<parameterNames.length;i++){
            if("publicTaskId".equals(parameterNames[i])) publicTaskId = (UUID) args[i];
            else if("publicCommentId".equals(parameterNames[i])) publicCommentId = (UUID)args[i];
            else if("loginMemberId".equals(parameterNames[i])) loginMemberId = (Long)args[i];
            else if("publicNotificationId".equals(parameterNames[i])) publicNotificationId = (UUID)args[i];
            else if("publicProjectId".equals(parameterNames[i])) publicProjectId = (UUID)args[i];
            else if("publicLabelId".equals(parameterNames[i])) publicLabelId = (UUID)args[i];
            else if("publicMemberId".equals(parameterNames[i])) publicMemberId = (UUID)args[i];
            else if(args[i] instanceof TaskRequest.Create)request = (TaskRequest.Create)args[i];
        }

        for(CheckOwnership checkOwnership: checkOwnerships){
            String domainType = checkOwnership.type();
            if("TASK".equals(domainType)){
                if(publicTaskId == null || loginMemberId == null){
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
                checkTaskOwner(publicTaskId,loginMemberId);
            }else if("SUB_TASK".equals(domainType)){
                if(request != null && request.publicParentId() != null && loginMemberId != null){
                    checkTaskOwner(request.publicParentId(),loginMemberId);
                }
            }else if("COMMENT".equals(domainType)){
                if(publicCommentId == null || loginMemberId == null){
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
                checkCommentOwner(publicCommentId,loginMemberId);
            }else if("MEMBER".equals(domainType)){
                if(loginMemberId == null){
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
                checkMemberOwner(publicMemberId,loginMemberId);
            }else if("NOTIFICATION".equals(domainType)){
                if(publicNotificationId == null || loginMemberId == null){
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
                checkNotificationOwner(publicNotificationId,loginMemberId);
            }else if("PROJECT".equals(domainType)) {
                if(publicProjectId == null || loginMemberId == null){
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
                checkProjectOwner(publicProjectId, loginMemberId);
            }else if("LABEL".equals(domainType)){
                if(publicLabelId == null || loginMemberId == null){
                    throw new BusinessException(ErrorCode.ACCESS_DENIED);
                }
                checkLabelOwner(publicLabelId,loginMemberId);
            }
        }
    }

    public void checkTaskOwner(UUID publicTaskId, Long memberId){
        if(!taskRepository.existsByPublicId(publicTaskId)){
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        if(!taskRepository.existsByPublicIdAndHasAccess(publicTaskId,memberId)){
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void checkCommentOwner(UUID publicCommentId, Long memberId){
        Comment comment = commentRepository.getOrThrowByPublicId(publicCommentId);
        if(!comment.getMember().getId().equals(memberId)){
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void checkLabelOwner(UUID publicLabelId, Long memberId){
        Label label = labelRepository.getOrThrowByPublicId(publicLabelId);
        if(!label.getMember().getId().equals(memberId)){
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void checkMemberOwner(UUID publicMemberId, Long memberId){
        Member member = memberRepository.getOrThrow(memberId);
        if(!member.getPublicId().equals(publicMemberId)){
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void checkNotificationOwner(UUID publicNotificationId, Long memberId){
        Notification notification = notificationRepository.getOrThrowByPublicId(publicNotificationId);
        if(!notification.getReceiver().getId().equals(memberId)){
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void checkProjectOwner(UUID publicProjectId, Long memberId){
        Project project = projectRepository.getOrThrowByProjectId(publicProjectId);
        Member member = memberRepository.getReferenceById(memberId);
        boolean isMember = projectMemberRepository.existsByProjectAndMember(project, member);
        if(!isMember){
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
