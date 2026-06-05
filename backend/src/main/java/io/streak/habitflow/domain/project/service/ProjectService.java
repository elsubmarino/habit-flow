package io.streak.habitflow.domain.project.service;

import io.streak.habitflow.domain.project.dto.ProjectRequest;
import io.streak.habitflow.domain.project.dto.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectResponse createProject(ProjectRequest projectRequest) {
        Project project = Project.builder()
                .name(projectRequest.getName())
                .color(projectRequest.getColor())
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    public List<ProjectResponse> getProjects(ProjectRequest projectRequest, UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<Project> projects = projectRepository.findByMemberEmail(email);
        return projects.stream()
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasks(TaskRequest taskRequest, UserDetails userDetails){
        List<Task> tasks = taskRepository.findByProjectId(taskRequest.getProjectId());
        return tasks.stream()
                .map(task -> TaskResponse.from(task,null))
                .collect(Collectors.toList());
    }

    public ProjectResponse updateProject(ProjectRequest projectRequest, Long id, UserDetails userDetails) {
        Project project = Project.builder()
                .id(id)
                .name(projectRequest.getName())
                .color(projectRequest.getColor())
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }
}
