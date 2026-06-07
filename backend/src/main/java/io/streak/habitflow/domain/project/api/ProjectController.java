package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.request.ProjectCreateRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody ProjectCreateRequest projectCreateRequest,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.createProject(projectCreateRequest, userDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectById(id,userDetails));
    }

    @GetMapping
    public ResponseEntity<List<ProjectListResponse>> getProjectsByMember(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectsByMember(userDetails));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@RequestBody ProjectCreateRequest projectCreateRequest,
                                                         @PathVariable("id") Long projectId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.updateProject(projectCreateRequest,projectId,userDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long projectId,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        projectService.deleteProject(projectId,userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTasksByProject(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProjectListResponse>> searchProjects(@AuthenticationPrincipal UserDetails userDetails,
                                                            @RequestParam("keyword") String keyword){
        return ResponseEntity.ok(projectService.searchProjects(keyword,userDetails));
    }
}
