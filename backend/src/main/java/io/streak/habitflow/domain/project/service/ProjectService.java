package io.streak.habitflow.domain.project.service;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.service.NotificationService;
import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.domain.project.dto.request.ProjectCreateRequest;
import io.streak.habitflow.domain.project.dto.request.ProjectInviteRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectMemberListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectMember;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.aop.CheckOwnership;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationService notificationService;

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest projectCreateRequest,
                                         Long memberId) {
        Project.ProjectBuilder projectBuilder = Project.builder()
                .name(projectCreateRequest.getName())
                .color(projectCreateRequest.getColor())
                .accessType(projectCreateRequest.getAccessType())
                .layoutType(projectCreateRequest.getLayoutType());

        if(projectCreateRequest.getParentId() != null){
            Project parentProject = projectRepository.findById(projectCreateRequest.getParentId())
                    .orElseThrow(()->new IllegalArgumentException("부모 프로젝트가 존재하지 않습니다."));
            projectBuilder.parent(parentProject);
        }

        Project project = projectBuilder.build();

        Project savedProject = projectRepository.save(project);

        Member member = memberRepository.getReferenceById(memberId);


        ProjectMember projectMember = ProjectMember.builder()
                .project(savedProject)
                .member(member)
                .build();
        projectMemberRepository.save(projectMember);

        if(projectCreateRequest.isFavorite()){
            Favorite favorite = Favorite.builder()
                    .targetType(TargetType.PROJECT)
                    .targetId(savedProject.getId())
                    .member(member)
                    .build();
            favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                    member.getId(),
                    TargetType.PROJECT,
                    savedProject.getId()
            ).orElseGet(()->favoriteRepository.save(favorite));
        }


        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                project.getId(),
                memberId,
                io.streak.habitflow.domain.task.type.TargetType.PROJECT,
                ActivityType.ADDED,
                "당신이 프로젝트 "+project.getName()+"을(를) 추가했습니다"
        ));

        return ProjectResponse.of(savedProject, projectCreateRequest.isFavorite());
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public void updateProject(ProjectCreateRequest projectCreateRequest, Long projectId, Long memberId) {
        Project project =  projectRepository.findById(projectId)
                .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));

        Member member = memberRepository.getReferenceById(memberId);

        Project parentProject = null;
        if(projectCreateRequest.getParentId() != null){
            parentProject = projectRepository.findById(projectCreateRequest.getParentId())
                    .orElseThrow(()->new IllegalArgumentException("부모 프로젝트가 존재하지 않습니다."));
        }


        project.updateProject(projectCreateRequest.getName(),
                projectCreateRequest.getColor(),
                projectCreateRequest.getAccessType(),
                projectCreateRequest.getLayoutType(),
                parentProject);

        if(projectCreateRequest.isFavorite()){
            Favorite favorite = Favorite.builder()
                    .targetType(TargetType.PROJECT)
                    .targetId(project.getId())
                    .member(member)
                    .build();
            favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                    member.getId(),
                    TargetType.PROJECT,
                    project.getId()
            ).orElseGet(()->favoriteRepository.save(favorite));
        }else{
            favoriteRepository.deleteByMemberIdAndTargetTypeAndTargetId(member.getId(),TargetType.PROJECT,project.getId());
        }

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                project.getId(),
                memberId,
                io.streak.habitflow.domain.task.type.TargetType.PROJECT,
                ActivityType.UPDATED,
                "당신이 프로젝트 "+project.getName()+"을(를) 변경했습니다"
        ));

    }

    public ProjectResponse getProjectById(Long projectId, Long memberId) {
       Project project = projectRepository.findById(projectId)
               .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));
       boolean isFavorite = false;
       Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
               memberId, TargetType.PROJECT, project.getId());
       if(favorite.isPresent()){
           isFavorite = true;
       }
       return ProjectResponse.of(project,isFavorite);
    }

    public List<ProjectListResponse> getProjectsByMember(Long memberId) {
        return projectRepository.findByMemberId(memberId);
    }



    @Transactional
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public void deleteProject(Long projectId, Long memberId) {
        Project project = projectRepository.getReferenceById(projectId);
        projectMemberRepository.deleteByProjectId(projectId);
        favoriteRepository.deleteByTargetTypeAndTargetId(TargetType.PROJECT, projectId);
        projectRepository.deleteById(projectId);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                project.getId(),
                memberId,
                io.streak.habitflow.domain.task.type.TargetType.PROJECT,
                ActivityType.DELETED,
                "당신이 프로젝트 "+project.getName()+"을(를)삭제했습니다"
        ));

    }

    public List<ProjectListResponse> searchProjects(String keyword, Long memberId, Pageable pageable) {
        return projectRepository.searchKeyword(keyword,memberId, pageable);
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public void invite(ProjectInviteRequest projectInviteRequest, Long memberId){
        Project project = projectRepository.findById(projectInviteRequest.getId())
                .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));

        Member inviter = memberRepository.findById(memberId)
                .orElseThrow(()->new IllegalArgumentException("초대자 정보가 올바르지 않습니다."));

        List<String> inviteEmails  = projectInviteRequest.getEmails();
        if(inviteEmails == null || inviteEmails.isEmpty()) return;

        List<Member> inviteMembers = memberRepository.findByEmailIn(inviteEmails);

        if(inviteMembers.size() != inviteEmails.size()){
            throw new IllegalArgumentException("존재하지 않는 이메일이 포함되어 있습니다.");
        }

        List<ProjectMember> projectMembers = inviteMembers.stream()
                .map(member->ProjectMember.builder()
                        .project(project)
                        .member(member)
                        .build())
                .toList();

        projectMemberRepository.saveAll(projectMembers);

        for(Member targetMember: inviteMembers){
            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .targetId(project.getId())
                    .notificationType(NotificationType.PROJECT)
                    .activityType(ActivityType.INVITED)
                    .customMessage(inviter.getName()+" 님이 ["+project.getName()+"] 프로젝트에 당신을 초대했습니다.")
                    .build();
            notificationService.createNotification(notificationRequest, targetMember.getId(),memberId );


            NotificationRequest joinedRequest = NotificationRequest.builder()
                    .targetId(project.getId())
                    .notificationType(NotificationType.PROJECT)
                    .activityType(ActivityType.JOINED)
                    .customMessage(targetMember.getName()+" 님이 ["+project.getName()+"] 프로젝트에 합류했습니다")
                    .build();
            notificationService.createNotification(joinedRequest, memberId, targetMember.getId());
        }


    }

    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public List<ProjectMemberListResponse> getProjectMembers(Long projectId,Long memberId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow();
        List<ProjectMember> projectMembers = projectMemberRepository.findByProject(project);
        return projectMembers.stream()
                .map(ProjectMemberListResponse::from)
                .toList();
    }
}
