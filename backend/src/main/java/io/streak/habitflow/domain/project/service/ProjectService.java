package io.streak.habitflow.domain.project.service;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.dto.request.ProjectCreateRequest;
import io.streak.habitflow.domain.project.dto.request.ProjectInviteRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectMemberListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectMember;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
        return ProjectResponse.from(savedProject, projectCreateRequest.isFavorite());
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public ProjectResponse updateProject(ProjectCreateRequest projectCreateRequest, Long projectId, Long memberId) {
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

        return ProjectResponse.from(project, projectCreateRequest.isFavorite());
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
       return ProjectResponse.from(project,isFavorite);
    }

    public List<ProjectListResponse> getProjectsByMember(Long memberId) {
        return projectRepository.findByMemberId(memberId);
    }



    @Transactional
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public void deleteProject(Long projectId, Long memberId) {
        projectMemberRepository.deleteByProjectId(projectId);
        favoriteRepository.deleteByTargetTypeAndTargetId(TargetType.PROJECT, projectId);
        projectRepository.deleteById(projectId);

    }

    public List<ProjectListResponse> searchProjects(String keyword, Long memberId) {
        return projectRepository.searchKeyword(keyword,memberId);
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public void invite(ProjectInviteRequest projectInviteRequest, Long memberId){
        Project project = projectRepository.findById(projectInviteRequest.getId())
                .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));

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
