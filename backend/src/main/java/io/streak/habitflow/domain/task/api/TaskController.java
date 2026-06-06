package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.TaskCreateRequest;
import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.dto.TaskResponse;
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

    /**
     * 테스크 생성
     * @param userDetails
     * @param file
     * @param taskCreateRequest
     * @return
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestPart(value = "file", required = false) MultipartFile file,
                                           @RequestPart("taskRequest") @Valid TaskCreateRequest taskCreateRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskCreateRequest, file, userDetails));
    }

    /**
     * 테스크 단건 조회
     * @param id
     * @param userDetails
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.readTask(id,userDetails));
    }

    /**
     * 테스크 업데이트
     * @param taskId
     * @param taskRequest
     * @param userDetails
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId, @RequestBody TaskRequest taskRequest
            ,@AuthenticationPrincipal UserDetails userDetails) {
        TaskResponse taskResponse = taskService.updateTask(taskId,taskRequest,userDetails);
        return ResponseEntity.ok(taskResponse);
    }

    /**
     * 테스크 삭제
     * @param id
     * @param userDetails
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        taskService.deleteTask(id,userDetails);
        return ResponseEntity.noContent().build();
    }

    /**
     * 테스크에 종속된 코멘트 다건 조회
     * @param userDetails
     * @return
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(commentService.getComments(id));
    }


}
