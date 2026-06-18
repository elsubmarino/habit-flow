package io.streak.habitflow.domain.project.service;

import io.streak.habitflow.domain.activitylog.dto.ChangeSet;
import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.dto.query.ProjectSummaryQuery;
import io.streak.habitflow.domain.project.dto.request.ProjectRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectMember;
import io.streak.habitflow.domain.project.event.ProjectInvitationEvent;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.aop.CheckOwnership;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
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

    @Transactional
    public ProjectResponse.Detail createProject(ProjectRequest.Create request,
                                         Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);

        long projectCount = projectMemberRepository.countByMember(member);
        if (projectCount > 500) {
            throw new IllegalArgumentException("보유하고 있는 프로젝트가 500개 이상이므로 더 이상 생성할 수 없습니다.");
        }

        Project.ProjectBuilder projectBuilder = Project.builder()
                .name(request.name())
                .color(request.color())
                .accessType(request.accessType())
                .layoutType(request.layoutType());

        if(request.parentId() != null){
            Project parentProject = projectRepository.findById(request.parentId())
                    .orElseThrow(()->new EntityNotFoundException("부모 프로젝트가 존재하지 않습니다."));
            projectBuilder.parent(parentProject);
        }

        Project project = projectBuilder.build();

        Project savedProject = projectRepository.save(project);



        ProjectMember projectMember = ProjectMember.builder()
                .project(savedProject)
                .member(member)
                .build();
        projectMemberRepository.save(projectMember);

        if(request.favorite()){
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
                project.getName(),
                Collections.emptyList()
        ));

        return ProjectResponse.Detail.of(savedProject, request.favorite());
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public void updateProject(ProjectRequest.Create request, Long projectId, Long memberId) {
        Project project =  projectRepository.getOrThrow(projectId);
        String oldProjectName = project.getName();

        Member member = memberRepository.getReferenceById(memberId);

        Project parentProject = null;
        if(request.parentId() != null){
            parentProject = projectRepository.findById(request.parentId())
                    .orElseThrow(()->new EntityNotFoundException("부모 프로젝트가 존재하지 않습니다."));
        }


        project.updateProject(request.name(),
                request.color(),
                request.accessType(),
                request.layoutType(),
                parentProject);

        if(request.favorite()){
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

        if(request.name() != null && !request.name().equals(oldProjectName)){
            List<ChangeSet> changeSets = new ArrayList<>();
            changeSets.add(new ChangeSet("name",oldProjectName,request.name()));
            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    project.getId(),
                    memberId,
                    io.streak.habitflow.domain.task.type.TargetType.PROJECT,
                    ActivityType.UPDATED,
                    request.name(),
                    changeSets
            ));
        }



    }

    public ProjectResponse.Detail getProjectById(Long projectId, Long memberId) {
       Project project = projectRepository.getOrThrow(projectId);
       boolean isFavorite = false;
       Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
               memberId, TargetType.PROJECT, project.getId());
       if(favorite.isPresent()){
           isFavorite = true;
       }
       return ProjectResponse.Detail.of(project,isFavorite);
    }

    public List<ProjectResponse.Summary> getProjectsByMember(Long memberId) {
        List<ProjectSummaryQuery> projectListQueries = projectRepository.findByMemberId(memberId);
        return projectListQueries.stream()
                .map(ProjectResponse.Summary::from)
                .toList();
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
                project.getName(),
                Collections.emptyList()
        ));

    }

    public List<ProjectResponse.Summary> searchProjects(String keyword, Long memberId, Pageable pageable) {
        List<ProjectSummaryQuery> projectListQueries = projectRepository.searchKeyword(keyword,memberId, pageable);
        return projectListQueries.stream()
                .map(ProjectResponse.Summary::from)
                .toList();
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public void invite(ProjectRequest.Invite inviteRequest, Long memberId){
        Project project = projectRepository.getOrThrow(inviteRequest.id());

        Member inviter = memberRepository.findById(memberId)
                .orElseThrow(()->new EntityNotFoundException("초대자 정보가 올바르지 않습니다."));

        List<String> inviteEmails  = inviteRequest.emails();
        if(inviteEmails == null || inviteEmails.isEmpty()) return;

        List<Member> inviteMembers = memberRepository.findByEmailIn(inviteEmails);

        if(inviteMembers.size() != inviteEmails.size()){
            throw new EntityNotFoundException("존재하지 않는 이메일이 포함되어 있습니다.");
        }

        List<ProjectMember> projectMembers = inviteMembers.stream()
                .map(member->ProjectMember.builder()
                        .project(project)
                        .member(member)
                        .build())
                .toList();

        projectMemberRepository.saveAll(projectMembers);

        List<ProjectInvitationEvent.MemberInfo> inviteeInfos = inviteMembers.stream()
                .map((Member m) ->new ProjectInvitationEvent.MemberInfo(m.getId(),m.getName()))
                .toList();

        applicationEventPublisher.publishEvent(new ProjectInvitationEvent(
                project.getId(),
                project.getName(),
                inviter.getId(),
                inviter.getName(),
                inviteeInfos
        ));
    }

    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public List<ProjectResponse.Member> getProjectMembers(Long projectId,Long memberId) {
        Project project = projectRepository.getOrThrow(projectId);
        List<ProjectMember> projectMembers = projectMemberRepository.findByProject(project);
        return projectMembers.stream()
                .map(ProjectResponse.Member::from)
                .toList();
    }
}
