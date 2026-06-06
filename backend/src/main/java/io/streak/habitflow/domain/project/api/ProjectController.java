package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.ProjectRequest;
import io.streak.habitflow.domain.project.dto.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.TaskResponse;
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
    public ResponseEntity<ProjectResponse> createProject(@RequestBody ProjectRequest projectRequest){
        return ResponseEntity.ok(projectService.createProject(projectRequest));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjectsByMember(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectsByMember(userDetails));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@RequestBody ProjectRequest projectRequest,@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.updateProject(projectRequest,id,userDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTasksByProject(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProjectResponse>> searchProjects(@AuthenticationPrincipal UserDetails userDetails,
                                                            @RequestParam("keyword") String keyword){
        return ResponseEntity.ok(projectService.searchProjects(keyword,userDetails));
    }
}
