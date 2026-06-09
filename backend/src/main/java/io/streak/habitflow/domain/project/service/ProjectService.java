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
import io.streak.habitflow.domain.task.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest projectCreateRequest,
                                         UserDetails userDetails) {
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

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다."));


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
    public ProjectResponse updateProject(ProjectCreateRequest projectCreateRequest, Long projectId, UserDetails userDetails) {
        Project project =  projectRepository.findById(projectId)
                .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        validateOwner(project,member);

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

    public ProjectResponse getProjectById(Long projectId, UserDetails userDetails) {
       Project project = projectRepository.findById(projectId)
               .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));
       Member member = memberRepository.findByEmail(userDetails.getUsername())
               .orElseThrow(()->new IllegalArgumentException("사용자가 존재하지 않습니다."));
       boolean isFavorite = false;
       Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
               member.getId(), TargetType.PROJECT, project.getId());
       if(favorite.isPresent()){
           isFavorite = true;
       }
       return ProjectResponse.from(project,isFavorite);
    }

    public List<ProjectListResponse> getProjectsByMember(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return projectRepository.findByEmail(email);
    }



    @Transactional
    public void deleteProject(Long projectId, UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(()->new IllegalArgumentException("유저가 존재하지 않습니다."));
        Project project = projectRepository.findById(projectId)
                        .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));
        validateOwner(project,member);
        projectMemberRepository.deleteByProjectId(projectId);
        favoriteRepository.deleteByTargetTypeAndTargetId(TargetType.PROJECT, projectId);
        projectRepository.deleteById(projectId);

    }

    public List<ProjectListResponse> searchProjects(String keyword, UserDetails userDetails) {
        return projectRepository.searchKeyword(keyword,userDetails.getUsername());
    }

    public void validateOwner(Project project, Member member){
        boolean isMember = projectMemberRepository.existsByProjectAndMember(project, member);
        if(!isMember){
            throw new IllegalStateException("해당 프로젝트에 대한 접근 권한이 없습니다.");
        }
    }

    public void invite(ProjectInviteRequest projectInviteRequest, UserDetails userDetails){
        Project project = projectRepository.findById(projectInviteRequest.getId())
                .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));
        Member _member = memberRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        validateOwner(project,_member);
        for(String email : projectInviteRequest.getEmails()){
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(()->new IllegalArgumentException("해당 이메일이 존재하지 않습니다."));
            ProjectMember projectMember = ProjectMember.builder()
                    .project(project)
                    .member(member)
                    .build();
            projectMemberRepository.save(projectMember);
        }
    }

    public List<ProjectMemberListResponse> getProjectMembers(Long projectId,UserDetails userDetails) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        validateOwner(project,member);
        List<ProjectMember> projectMembers = projectMemberRepository.findByProject(project);
        return projectMembers.stream()
                .map(ProjectMemberListResponse::from)
                .toList();
    }
}
