package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateDueDateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task-instances")
@RequiredArgsConstructor
public class TaskInstanceController {
    private final TaskService taskService;
    private final CommentService commentService;

    @GetMapping("/{taskInstanceId}")
    @Operation(summary = "테스크 상세 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 상세 조회 성공")})
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long taskInstanceId,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.getTaskById(taskInstanceId,userPrincipal.getMemberId()));
    }


    @GetMapping("/inbox")
    @Operation(summary = "관리함 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "관리함 테스크 조회 성공")})
    public ResponseEntity<ScrollResponse<TaskListResponse>> getInboxTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId,
                                                                          @PageableDefault(size=20)  Pageable pageable) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.INBOX);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition, userPrincipal.getMemberId(),pageable));
    }

    @GetMapping("/today")
    @Operation(summary = "오늘 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "오늘 테스크 조회 성공")})
    public ResponseEntity<ScrollResponse<TaskListResponse>> getTodayTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId,
                                                                          @PageableDefault(size=20) Pageable pageable) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.TODAY);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userPrincipal.getMemberId(), pageable));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "다가오는 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "다가오는 테스크 조회 성공")})
    public ResponseEntity<ScrollResponse<TaskListResponse>> getUpcomingTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                             @RequestParam(value="lastTaskId",required = false) Long lastTaskId,
                                                                             @PageableDefault(size = 20) Pageable pageable) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setLastTaskId(lastTaskId);

        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userPrincipal.getMemberId(), pageable));
    }

    @PatchMapping("/{taskInstanceId}/due-date")
    @Operation(summary = "만료일 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "만료일 업데이트 성공")})
    public ResponseEntity<TaskResponse> updateTaskDueDate(@PathVariable Long taskInstanceId,
                                                          @RequestBody TaskUpdateDueDateRequest taskUpdateDueDateRequest,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTaskDueDate(taskInstanceId, taskUpdateDueDateRequest, userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskInstanceId}/toggle")
    @Operation(summary = "테스크 토글(완료/미완료)")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 토글(완료/미완료) 성공")})
    public ResponseEntity<TaskResponse> toggleCompletion(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @PathVariable Long taskInstanceId) {
        return ResponseEntity.ok(taskService.toggleCompletion(taskInstanceId,userPrincipal.getMemberId()));
    }


}
