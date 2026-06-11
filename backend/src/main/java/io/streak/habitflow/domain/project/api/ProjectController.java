package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.request.ProjectCreateRequest;
import io.streak.habitflow.domain.project.dto.request.ProjectInviteRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectMemberListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(projectCreateRequest, userPrincipal.getMemberId()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(projectService.getProjectById(projectId,userPrincipal.getMemberId()));
    }

    @GetMapping
    public ResponseEntity<List<ProjectListResponse>> getProjectsByMember(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(projectService.getProjectsByMember(userPrincipal.getMemberId()));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@RequestBody ProjectCreateRequest projectCreateRequest,
                                                         @PathVariable Long projectId,
                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(projectService.updateProject(projectCreateRequest,projectId,userPrincipal.getMemberId()));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        projectService.deleteProject(projectId,userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<List<TaskListResponse>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProjectListResponse>> searchProjects(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                    @RequestParam("keyword") String keyword,
                                                                    @PageableDefault(size=20) Pageable pageable){
        return ResponseEntity.ok(projectService.searchProjects(keyword,userPrincipal.getMemberId(),pageable));
    }

    @PostMapping("/invitation")
    public ResponseEntity<Void> invite(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       @RequestBody ProjectInviteRequest projectInviteRequest){
        projectService.invite(projectInviteRequest, userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberListResponse>> getProjectMembers(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                   @PathVariable Long projectId){
        return ResponseEntity.ok(projectService.getProjectMembers(projectId,userPrincipal.getMemberId()));
    }
}
