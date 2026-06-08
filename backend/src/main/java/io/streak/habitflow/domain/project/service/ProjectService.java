package io.streak.habitflow.domain.project.service;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.dto.request.ProjectCreateRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectUser;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.project.repository.ProjectUserRepository;
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
    private final ProjectUserRepository projectUserRepository;
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


        ProjectUser projectUser = ProjectUser.builder()
                .project(savedProject)
                .member(member)
                .build();
        projectUserRepository.save(projectUser);

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
        List<Project> projects = projectRepository.findByMemberEmail(email);
        return projects.stream()
                .map(ProjectListResponse::from)
                .toList();
    }



    @Transactional
    public void deleteProject(Long projectId, UserDetails userDetails) {
        memberRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(()->new IllegalArgumentException("유저가 존재하지 않습니다."));
        projectRepository.findById(projectId)
                        .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));
        projectUserRepository.deleteByProjectId(projectId);
        favoriteRepository.deleteByTargetTypeAndTargetId(TargetType.PROJECT, projectId);
        projectRepository.deleteById(projectId);

    }

    public List<ProjectListResponse> searchProjects(String keyword, UserDetails userDetails) {
        return projectRepository.searchKeyword(keyword,userDetails.getUsername());
    }
}
