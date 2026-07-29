package io.streak.habitflow.domain.project.api;

import io.streak.habitflow.domain.project.dto.request.ProjectRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.service.ProjectService;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.global.common.constant.PageSizeConstants;
import io.streak.habitflow.global.web.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/{publicProjectId}")
    @Operation(summary = "프로젝트 상세 조회")
    public ResponseEntity<ProjectResponse.Detail> getProjectById(@PathVariable UUID publicProjectId,
                                                          @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(projectService.getProjectByPublicId(publicProjectId,loginMemberId));
    }

    @GetMapping
    @Operation(summary = "프로젝트 다건 조회")
    public ResponseEntity<List<ProjectResponse.Summary>> getProjectsByMember(@LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(projectService.getProjectsByMember(loginMemberId));
    }

    @PutMapping("/{publicProjectId}")
    @Operation(summary = "프로젝트 업데이트")
    public ResponseEntity<ProjectResponse.Detail> updateProject(@RequestBody ProjectRequest.Update request,
                                                 @PathVariable UUID publicProjectId,
                                                 @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(projectService.updateProject(request,publicProjectId,loginMemberId));
    }

    @DeleteMapping("/{publicProjectId}")
    @Operation(summary = "프로젝트 삭제")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID publicProjectId,
                                              @LoginMemberId Long loginMemberId) {
        projectService.deleteProject(publicProjectId,loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/tasks")
    @Operation(summary = "프로젝트에 딸린 테스크 조회")
    public ResponseEntity<Slice<TaskResponse.Summary>> getTasksByProject(@PathVariable UUID projectId,
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

    @PostMapping("/{publicProjectID}/invitation")
    @Operation(summary = "프로젝트 초대 이메일 발송")
    public ResponseEntity<Void> inviteMembers(@LoginMemberId Long loginMemberId,
                                              @RequestBody ProjectRequest.Invite request,
                                              @PathVariable UUID publicProjectID){
        projectService.inviteMembers(request, publicProjectID, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitation/accept")
    @Operation(summary = "프로젝트 초대 링크 수락")
    public ResponseEntity<Void> acceptInvitation(@LoginMemberId Long loginMemberId,
                                       @RequestParam("token") String token){
        projectService.acceptInvitation(token, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{publicProjectId}/members")
    @Operation(summary = "프로젝트 참여 회원 보기")
    public ResponseEntity<List<ProjectResponse.Member>> getProjectMembers(@LoginMemberId Long loginMemberId,
                                                                   @PathVariable UUID publicProjectId){
        List<ProjectResponse.Member> list =  projectService.getProjectMembers(publicProjectId,loginMemberId);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{publicProjectId}/members")
    @Operation(summary = "프로젝트에서 해당 멤버 제거")
    @ApiResponses(value={@ApiResponse(responseCode = "204",description = "프로젝트에서 해당 멤버 제거 성공")})
    public ResponseEntity<Void> deleteProjectMember(@LoginMemberId Long loginMemberId,
                                                                          @RequestBody ProjectRequest.DeleteMember request,
                                                                          @PathVariable UUID publicProjectId){
        projectService.deleteProjectMember(publicProjectId,loginMemberId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{publicProjectId}/sort-order")
    @Operation(summary = "정렬순서 변경")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "정렬순서 변경 성공")})
    public ResponseEntity<ProjectResponse.Summary> updateSortOrder(@PathVariable UUID publicProjectId,
                                                                 @RequestBody @Valid ProjectRequest.UpdateSortOrder request,
                                                                 @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(projectService.updateSortOrder(publicProjectId, request, loginMemberId));
    }
}
