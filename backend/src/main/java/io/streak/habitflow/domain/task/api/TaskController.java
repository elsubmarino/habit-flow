package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.*;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                           @RequestPart(value = "file", required = false) MultipartFile file,
                                           @RequestPart("taskRequest") @Valid TaskCreateRequest taskCreateRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskCreateRequest, file, userPrincipal.getMemberId()));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long taskId,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.getTaskById(taskId,userPrincipal.getMemberId()));
    }

    @GetMapping("/inbox")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getInboxTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.INBOX);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition, userPrincipal.getMemberId()));
    }

    @GetMapping("/today")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getTodayTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.TODAY);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userPrincipal.getMemberId()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getUpcomingTasks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                             @RequestParam(value="lastTaskId",required = false) Long lastTaskId) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setLastTaskId(lastTaskId);

        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userPrincipal.getMemberId()));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId,
                                                   @RequestBody TaskUpdateRequest taskUpdateRequest,
                                                   @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTask(taskId, taskUpdateRequest,userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/due-date")
    public ResponseEntity<TaskResponse> updateTaskDueDate(@PathVariable Long taskId,
                                                          @RequestBody TaskUpdateDueDateRequest taskUpdateDueDateRequest,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTaskDueDate(taskId, taskUpdateDueDateRequest.getDueDate(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/priority")
    public ResponseEntity<TaskResponse> updatePriority(@PathVariable Long taskId,
                                                          @RequestBody TaskUpdatePriorityRequest taskUpdatePriorityRequest,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updatePriority(taskId, taskUpdatePriorityRequest.getTaskPriorityType(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/labels")
    public ResponseEntity<TaskResponse> updateLabels(@PathVariable Long taskId,
                                                       @RequestBody TaskUpdateLabelRequest taskUpdateLabelRequest,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTaskLabels(taskId, taskUpdateLabelRequest.getLabelIds(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/project")
    public ResponseEntity<TaskResponse> updateProject(@PathVariable Long taskId,
                                                      @RequestBody TaskUpdateProjectRequest taskUpdateProjectRequest,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateProject(taskId, taskUpdateProjectRequest.getProjectId(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        taskService.deleteTask(taskId,userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId));
    }

    @PatchMapping("/{taskId}/toggle")
    public ResponseEntity<TaskResponse> toggleCompletion(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.toggleCompletion(taskId,userPrincipal.getMemberId()));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTaskCount(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                             @RequestParam(required = false) TaskFilterType taskFilterType) {
        long totalCount = taskService.getTaskCount(taskFilterType,userPrincipal.getMemberId());
        return ResponseEntity.ok(totalCount);
    }


}
