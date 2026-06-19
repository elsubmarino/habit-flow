package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.request.ProjectRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.streak.habitflow.global.common.constant.PageSizeConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                                                         @LoginMemberId Long loginMemberId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request, loginMemberId));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 상세 조회")
    public ResponseEntity<ProjectResponse.Detail> getProjectById(@PathVariable Long projectId,
                                                          @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId,loginMemberId));
    }

    @GetMapping
    @Operation(summary = "프로젝트 다건 조회")
    public ResponseEntity<List<ProjectResponse.Summary>> getProjectsByMember(@LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(projectService.getProjectsByMember(loginMemberId));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "프로젝트 업데이트")
    public ResponseEntity<Void> updateProject(@RequestBody ProjectRequest.Create request,
                                                         @PathVariable Long projectId,
                                                         @LoginMemberId Long loginMemberId) {
        projectService.updateProject(request,projectId,loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                              @LoginMemberId Long loginMemberId) {
        projectService.deleteProject(projectId,loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/tasks")
    @Operation(summary = "프로젝트에 딸린 테스크 조회")
    public ResponseEntity<Slice<TaskResponse.Summary>> getTasksByProject(@PathVariable Long projectId,
                                                                         @LoginMemberId Long loginMemberId,
                                                                         @PageableDefault(size= PageSizeConstants.CURSOR_PAGING_NORMAL) Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId, loginMemberId, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "프로젝트 검색")
    public ResponseEntity<List<ProjectResponse.Summary>> searchProjects(@LoginMemberId Long loginMemberId,
                                                                        @RequestParam("keyword") String keyword,
                                                                        @PageableDefault(size=PageSizeConstants.CURSOR_PAGING_SMALL) Pageable pageable){
        return ResponseEntity.ok(projectService.searchProjects(keyword,loginMemberId,pageable));
    }

    @PostMapping("/{projectId}/invitation")
    @Operation(summary = "프로젝트 초대 이메일 발송")
    public ResponseEntity<Void> invite(@LoginMemberId Long loginMemberId,
                                       @RequestBody ProjectRequest.Invite request,
                                       @PathVariable Long projectId){
        projectService.invite(request, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitation/accept")
    @Operation(summary = "프로젝트 초대 링크 수락")
    public ResponseEntity<Void> acceptInvitation(@LoginMemberId Long loginMemberId,
                                       @RequestParam("token") String token){
        projectService.acceptInvitation(token, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectResponse.Member>> getProjectMembers(@LoginMemberId Long loginMemberId,
                                                                   @PathVariable Long projectId){
        return ResponseEntity.ok(projectService.getProjectMembers(projectId,loginMemberId));
    }
}
