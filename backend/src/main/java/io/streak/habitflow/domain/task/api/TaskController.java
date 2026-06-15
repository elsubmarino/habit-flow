package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final CommentService commentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "테스크 생성")
    @ApiResponses(value={
            @ApiResponse(responseCode = "201",description = "테스크 생성 성공")
    })
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                   @RequestPart(value = "file", required = false) MultipartFile file,
                                                   @RequestPart("taskRequest") @Valid TaskRequest.Create request){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, file, userPrincipal.getMemberId()));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "테스크 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 업데이트 성공")})
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId,
                                                   @RequestBody TaskRequest.Update request,
                                                   @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTask(taskId, request,userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/priority")
    @Operation(summary = "우선순위 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "우선순위 업데이트 성공")})
    public ResponseEntity<TaskResponse> updatePriority(@PathVariable Long taskId,
                                                          @RequestBody TaskRequest.UpdatePriority request,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updatePriority(taskId, request.taskPriorityType(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/labels")
    @Operation(summary = "라벨 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "라벨 업데이트 성공")})
    public ResponseEntity<TaskResponse> updateLabels(@PathVariable Long taskId,
                                                       @RequestBody TaskRequest.UpdateLabel request,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTaskLabels(taskId, request.labelIds(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/project")
    @Operation(summary = "테스크가 속한 프로젝트 이동")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크가 속한 프로젝트 이동 성공")})
    public ResponseEntity<TaskResponse> updateProject(@PathVariable Long taskId,
                                                      @RequestBody TaskRequest.UpdateProject request,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateProject(taskId, request.projectId(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "테스크 삭제")
    @ApiResponses(value={@ApiResponse(responseCode = "204",description = "테스크 삭제 성공")})
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        taskService.deleteTask(taskId,userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/comments")
    @Operation(summary = "테스크에 속한 댓글 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크에 속한 댓글 조회 성공")})
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId));
    }

    @GetMapping("/count")
    @Operation(summary = "테스크 건수 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 건수 조회 성공")})
    public ResponseEntity<Long> getTaskCount(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                             @RequestParam(required = false) TaskFilterType taskFilterType) {
        long totalCount = taskService.getTaskCount(taskFilterType,userPrincipal.getMemberId());
        return ResponseEntity.ok(totalCount);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "테스크 상세 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 상세 조회 성공")})
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long taskId,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.getTaskById(taskId,userPrincipal.getMemberId()));
    }


    @GetMapping("/inbox")
    @Operation(summary = "관리함 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "관리함 테스크 조회 성공")})
    public ResponseEntity<ScrollResponse<TaskListResponse>> getInboxTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @PageableDefault(size=20) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.INBOX
        );

        return ResponseEntity.ok(taskService.getTasks(searchCondition, userPrincipal.getMemberId(),pageable));
    }

    @GetMapping("/today")
    @Operation(summary = "오늘 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "오늘 테스크 조회 성공")})
    public ResponseEntity<ScrollResponse<TaskListResponse>> getTodayTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @PageableDefault(size=20) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.TODAY
        );
        return ResponseEntity.ok(taskService.getTasks(searchCondition,userPrincipal.getMemberId(), pageable));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "다가오는 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "다가오는 테스크 조회 성공")})
    public ResponseEntity<ScrollResponse<TaskListResponse>> getUpcomingTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                             @PageableDefault(size = 20) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.UPCOMING
        );

        return ResponseEntity.ok(taskService.getTasks(searchCondition,userPrincipal.getMemberId(), pageable));
    }

    @PatchMapping("/{taskId}/due-date")
    @Operation(summary = "만료일 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "만료일 업데이트 성공")})
    public ResponseEntity<TaskResponse> updateTaskDueDate(@PathVariable Long taskId,
                                                          @RequestBody TaskRequest.UpdateDueDate request,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTaskDueDate(taskId, request, userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/toggle")
    @Operation(summary = "테스크 토글(완료/미완료)")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 토글(완료/미완료) 성공")})
    public ResponseEntity<TaskResponse> toggleCompletion(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.toggleCompletion(taskId,userPrincipal.getMemberId()));
    }

}
