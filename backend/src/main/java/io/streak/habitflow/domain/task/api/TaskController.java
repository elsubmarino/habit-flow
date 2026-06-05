package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.project.dto.ProjectRequest;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.task.dto.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
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
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createTask(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestPart(value = "file", required = false) MultipartFile file,
                                           @RequestPart("taskRequest") @Valid TaskCreateRequest taskCreateRequest){
        taskService.createTask(taskCreateRequest, file, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        TaskResponse result = taskService.readTask(id,userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaskById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        taskService.deleteTask(id,userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/project/{id}")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long projectId) {
        List<TaskResponse> taskResponses = taskService.getTasksByProject(projectId,userDetails);
        return ResponseEntity.ok(taskResponses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId, @RequestBody TaskRequest taskRequest
            ,@AuthenticationPrincipal UserDetails userDetails) {
        TaskResponse taskResponse = taskService.updateTask(taskId,taskRequest,userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(taskResponse);
    }


}
