package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.*;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class TaskMasterController {
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
                                                   @RequestPart("taskRequest") @Valid TaskCreateRequest taskCreateRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskCreateRequest, file, userPrincipal.getMemberId()));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "테스크 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 업데이트 성공")})
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId,
                                                   @RequestBody TaskUpdateRequest taskUpdateRequest,
                                                   @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTask(taskId, taskUpdateRequest,userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/priority")
    @Operation(summary = "우선순위 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "우선순위 업데이트 성공")})
    public ResponseEntity<TaskResponse> updatePriority(@PathVariable Long taskId,
                                                          @RequestBody TaskUpdatePriorityRequest taskUpdatePriorityRequest,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updatePriority(taskId, taskUpdatePriorityRequest.getTaskPriorityType(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/labels")
    @Operation(summary = "라벨 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "라벨 업데이트 성공")})
    public ResponseEntity<TaskResponse> updateLabels(@PathVariable Long taskId,
                                                       @RequestBody TaskUpdateLabelRequest taskUpdateLabelRequest,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateTaskLabels(taskId, taskUpdateLabelRequest.getLabelIds(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}/project")
    @Operation(summary = "테스크가 속한 프로젝트 이동")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크가 속한 프로젝트 이동 성공")})
    public ResponseEntity<TaskResponse> updateProject(@PathVariable Long taskId,
                                                      @RequestBody TaskUpdateProjectRequest taskUpdateProjectRequest,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        TaskResponse taskResponse = taskService.updateProject(taskId, taskUpdateProjectRequest.getProjectId(), userPrincipal.getMemberId());
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "테스크 삭제")
    @ApiResponses(value={@ApiResponse(responseCode = "204",description = "테스크 삭제 성공")})
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
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

}
