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
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long taskInstanceId,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.getTaskById(taskInstanceId,userPrincipal.getMemberId()));
    }


    @GetMapping("/inbox")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getInboxTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId,
                                                                          @PageableDefault(size=20)  Pageable pageable) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.INBOX);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition, userPrincipal.getMemberId(),pageable));
    }

    @GetMapping("/today")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getTodayTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId,
                                                                          @PageableDefault(size=20) Pageable pageable) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.TODAY);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userPrincipal.getMemberId(), pageable));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getUpcomingTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                             @RequestParam(value="lastTaskId",required = false) Long lastTaskId,
                                                                             @PageableDefault(size = 20) Pageable pageable) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setLastTaskId(lastTaskId);

        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userPrincipal.getMemberId(), pageable));
    }

    @PatchMapping("/{taskInstanceId}/due-date")
    public ResponseEntity<TaskResponse> updateTaskDueDate(@PathVariable Long taskInstanceId,
                                                          @RequestBody TaskUpdateDueDateRequest taskUpdateDueDateRequest,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTaskDueDate(taskInstanceId, taskUpdateDueDateRequest, userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskInstanceId}/toggle")
    public ResponseEntity<TaskResponse> toggleCompletion(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @PathVariable Long taskInstanceId) {
        return ResponseEntity.ok(taskService.toggleCompletion(taskInstanceId,userPrincipal.getMemberId()));
    }


}
