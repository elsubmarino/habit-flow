package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.request.ProjectCreateRequest;
import io.streak.habitflow.domain.project.dto.request.ProjectInviteRequest;
import io.streak.habitflow.domain.project.dto.request.ProjectMemberRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectMemberListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @Operation(
            summary = "프로젝트 생성")
    @ApiResponses(value={
            @ApiResponse(responseCode = "201",description = "프로젝트 생성 성공")
    })
    public ResponseEntity<ProjectResponse> createProject(@RequestBody ProjectCreateRequest projectCreateRequest,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(projectCreateRequest, userDetails));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectById(projectId,userDetails));
    }

    @GetMapping
    public ResponseEntity<List<ProjectListResponse>> getProjectsByMember(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectsByMember(userDetails));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@RequestBody ProjectCreateRequest projectCreateRequest,
                                                         @PathVariable Long projectId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.updateProject(projectCreateRequest,projectId,userDetails));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        projectService.deleteProject(projectId,userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<List<TaskListResponse>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProjectListResponse>> searchProjects(@AuthenticationPrincipal UserDetails userDetails,
                                                            @RequestParam("keyword") String keyword){
        return ResponseEntity.ok(projectService.searchProjects(keyword,userDetails));
    }

    @PostMapping("/invitation")
    public ResponseEntity<Void> invite(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody ProjectInviteRequest projectInviteRequest){
        projectService.invite(projectInviteRequest, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberListResponse>> getProjectMembers(@AuthenticationPrincipal UserDetails userDetails,
                                                                   @PathVariable Long projectId){
        return ResponseEntity.ok(projectService.getProjectMembers(projectId,userDetails));
    }
}
