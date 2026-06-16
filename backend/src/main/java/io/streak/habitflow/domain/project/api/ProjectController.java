package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.request.ProjectRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    public ResponseEntity<ProjectResponse.Detail> createProject(@RequestBody ProjectRequest.Create request,
                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request, userPrincipal.getMemberId()));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 상세 조회")
    public ResponseEntity<ProjectResponse.Detail> getProjectById(@PathVariable Long projectId,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(projectService.getProjectById(projectId,userPrincipal.getMemberId()));
    }

    @GetMapping
    @Operation(summary = "프로젝트 다건 조회")
    public ResponseEntity<List<ProjectResponse.List>> getProjectsByMember(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(projectService.getProjectsByMember(userPrincipal.getMemberId()));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "프로젝트 업데이트")
    public ResponseEntity<Void> updateProject(@RequestBody ProjectRequest.Create request,
                                                         @PathVariable Long projectId,
                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        projectService.updateProject(request,projectId,userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        projectService.deleteProject(projectId,userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/tasks")
    @Operation(summary = "프로젝트에 딸린 테스크 조회")
    public ResponseEntity<Slice<TaskResponse.List>> getTasksByProject(@PathVariable Long projectId,
                                                                 @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                 @PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId, userPrincipal.getMemberId(), pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "프로젝트 검색")
    public ResponseEntity<List<ProjectResponse.List>> searchProjects(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                    @RequestParam("keyword") String keyword,
                                                                    @PageableDefault(size=20) Pageable pageable){
        return ResponseEntity.ok(projectService.searchProjects(keyword,userPrincipal.getMemberId(),pageable));
    }

    @PostMapping("/invitation")
    public ResponseEntity<Void> invite(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       @RequestBody ProjectRequest.Invite request){
        projectService.invite(request, userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectResponse.Member>> getProjectMembers(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                   @PathVariable Long projectId){
        return ResponseEntity.ok(projectService.getProjectMembers(projectId,userPrincipal.getMemberId()));
    }
}
