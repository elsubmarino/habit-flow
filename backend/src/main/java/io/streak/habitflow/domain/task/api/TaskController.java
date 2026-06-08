package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
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

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.getTaskById(id,userDetails));
    }

    @GetMapping("/today")
    public ResponseEntity<List<TaskListResponse>> getTodayTasks(@AuthenticationPrincipal UserDetails userDetails) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setFilterType("TODAY");
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userDetails));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TaskListResponse>> getUpcomingTasks(@AuthenticationPrincipal UserDetails userDetails) {
        TaskSearchCondition taskSearchCondition = new TaskSearchCondition();
        taskSearchCondition.setFilterType("UPCOMING");
        return ResponseEntity.ok(taskService.getTasks(taskSearchCondition,userDetails));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable("id") Long taskId,
                                                   @RequestBody TaskUpdateRequest taskUpdateRequest,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        TaskResponse taskResponse = taskService.updateTask(taskId, taskUpdateRequest,userDetails);
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        taskService.deleteTask(id,userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(commentService.getComments(id));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TaskResponse> toggleCompletion(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable("id") Long taskId) {
        return ResponseEntity.ok(taskService.toggleCompletion(taskId,userDetails));
    }


}
