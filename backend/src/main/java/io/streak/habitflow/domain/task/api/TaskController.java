package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestPart(value = "file", required = false) MultipartFile file,
                                           @RequestPart("taskRequest") @Valid TaskCreateRequest taskCreateRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskCreateRequest, file, userDetails));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long taskId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.getTaskById(taskId,userDetails));
    }

    @GetMapping("/inbox")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getInboxTasks(@AuthenticationPrincipal UserDetails userDetails,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.INBOX);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userDetails));
    }

    @GetMapping("/today")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getTodayTasks(@AuthenticationPrincipal UserDetails userDetails,
                                                                          @RequestParam(value="lastTaskId",required = false) Long lastTaskId) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.TODAY);
        taskSearchCondition.setLastTaskId(lastTaskId);
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userDetails));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ScrollResponse<TaskListResponse>> getUpcomingTasks(@AuthenticationPrincipal UserDetails userDetails,
                                                                             @RequestParam(value="lastTaskId",required = false) Long lastTaskId) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setTaskFilterType(TaskFilterType.UPCOMING);
        taskSearchCondition.setLastTaskId(lastTaskId);

        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userDetails));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId,
                                                   @RequestBody TaskUpdateRequest taskUpdateRequest,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        TaskResponse taskResponse = taskService.updateTask(taskId, taskUpdateRequest,userDetails);
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId, @AuthenticationPrincipal UserDetails userDetails) {
        taskService.deleteTask(taskId,userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId));
    }

    @PatchMapping("/{taskId}/toggle")
    public ResponseEntity<TaskResponse> toggleCompletion(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.toggleCompletion(taskId,userDetails));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTaskCount(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam(required = false) TaskFilterType taskFilterType) {
        long totalCount = taskService.getTaskCount(taskFilterType,userDetails.getUsername());
        return ResponseEntity.ok(totalCount);
    }


}
