package io.streak.habitflow.domain.project.service;

import io.streak.habitflow.domain.project.dto.ProjectRequest;
import io.streak.habitflow.domain.project.dto.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    /**
     * 프로젝트 생성
     * @param projectRequest
     * @return
     */
    @Transactional
    public ProjectResponse createProject(ProjectRequest projectRequest) {
        Project project = Project.builder()
                .name(projectRequest.getName())
                .color(projectRequest.getColor())
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    /**
     * 프로젝트 다건 조회
     * @param projectRequest
     * @param userDetails
     * @return
     */
    public List<ProjectResponse> getProjects(UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<Project> projects = projectRepository.findByMemberEmail(email);
        return projects.stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * 프로젝트 업데이트
     * @param projectRequest
     * @param id
     * @param userDetails
     * @return
     */
    @Transactional
    public ProjectResponse updateProject(ProjectRequest projectRequest, Long id, UserDetails userDetails) {
        Project project =  projectRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("프로젝트가 존재하지 않습니다."));
        project.updateProject(projectRequest.getName(),projectRequest.getColor());
        return ProjectResponse.from(project);
    }

    /**
     * 프로젝트 삭제
     * @param id
     */
    @Transactional
    public void deleteProject(Long id){
        projectRepository.deleteById(id);
    }
}
