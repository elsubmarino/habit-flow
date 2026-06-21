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
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


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
    public void validateOwnership(JoinPoint joinPoint) throws AccessDeniedException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        CheckOwnership[] checkOwnerships = method.getAnnotationsByType(CheckOwnership.class);

        String[]parameterNames = methodSignature.getParameterNames();
        Object[]args=joinPoint.getArgs();

        Long taskId = null;
        Long commentId = null;
        Long loginMemberId = null;
        Long notificationId = null;
        Long projectId = null;
        Long labelId = null;
        TaskRequest.Create request = null;

        for(int i=0;i<parameterNames.length;i++){
            if("taskId".equals(parameterNames[i])) taskId = (Long)args[i];
            else if("commentId".equals(parameterNames[i])) commentId = (Long)args[i];
            else if("loginMemberId".equals(parameterNames[i])) loginMemberId = (Long)args[i];
            else if("notificationId".equals(parameterNames[i])) notificationId = (Long)args[i];
            else if("projectId".equals(parameterNames[i])) projectId = (Long)args[i];
            else if("labelId".equals(parameterNames[i])) labelId = (Long)args[i];
            else if(args[i] instanceof TaskRequest.Create)request = (TaskRequest.Create)args[i];

        }

        for(CheckOwnership checkOwnership: checkOwnerships){
            String domainType = checkOwnership.type();
            if("TASK".equals(domainType) && taskId != null && loginMemberId != null){
                checkTaskOwner(taskId,loginMemberId);
            }else if("SUB_TASK".equals(domainType)){
                if(request != null && request.parentId() != null && loginMemberId != null){
                    checkTaskOwner(hashidsProvider.decode(request.parentId()),loginMemberId);
                }
            }else if("COMMENT".equals(domainType) && commentId != null && loginMemberId != null){
                checkCommentOwner(commentId,loginMemberId);
            }else if("MEMBER".equals(domainType) && loginMemberId != null){
                checkMemberOwner(loginMemberId,loginMemberId);
            }else if("NOTIFICATION".equals(domainType) && notificationId != null && loginMemberId != null){
                checkNotificationOwner(notificationId,loginMemberId);
            }else if("PROJECT".equals(domainType) && projectId != null && loginMemberId != null) {
                checkProjectOwner(projectId, loginMemberId);
            }else if("LABEL".equals(domainType) && labelId != null && loginMemberId != null){
                checkLabelOwner(labelId,loginMemberId);
            }
        }
    }

    public void checkTaskOwner(Long taskId, Long memberId){
        if(!taskRepository.existsById(taskId)){
            throw new AccessDeniedException("존재하지 않는 테스크입니다.");
        }

        if(!taskRepository.existsByIdAndHasAccess(taskId,memberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }
    }

    public void checkCommentOwner(Long commentId, Long memberId){
        Comment comment = commentRepository.getOrThrow(commentId);
        if(!comment.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }
    }

    public void checkLabelOwner(Long labelId, Long memberId){
        Label label = labelRepository.getOrThrow(labelId);
        if(!label.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }
    }

    public void checkMemberOwner(Long loginMemberId, Long memberId){
        Member member = memberRepository.getOrThrow(memberId);
        if(!member.getId().equals(loginMemberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }
    }

    public void checkNotificationOwner(Long notificationId, Long memberId){
        Notification notification = notificationRepository.getOrThrow(notificationId);
        if(!notification.getReceiver().getId().equals(memberId)){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }
    }

    public void checkProjectOwner(Long projectId, Long memberId){
        Project project = projectRepository.getOrThrow(projectId);
        Member member = memberRepository.getReferenceById(memberId);
        boolean isMember = projectMemberRepository.existsByProjectAndMember(project, member);
        if(!isMember){
            throw new AccessDeniedException("해당 자원에 대한 권한이 없습니다.");
        }
    }
}
