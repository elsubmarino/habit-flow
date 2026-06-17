package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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
    public ResponseEntity<TaskResponse.Detail> createTask(@LoginMemberId Long loginMemberId,
                                                   @RequestPart(value = "file", required = false) MultipartFile file,
                                                   @RequestPart("taskRequest") @Valid TaskRequest.Create request){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, file, loginMemberId));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "테스크 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 업데이트 성공")})
    public ResponseEntity<Void> updateTask(@PathVariable Long taskId,
                                                   @RequestBody TaskRequest.Update request,
                                                   @LoginMemberId Long loginMemberId) {
        taskService.updateTask(taskId, request,loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/priority")
    @Operation(summary = "우선순위 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "우선순위 업데이트 성공")})
    public ResponseEntity<Void> updatePriority(@PathVariable Long taskId,
                                                          @RequestBody TaskRequest.UpdatePriority request,
                                                          @LoginMemberId Long loginMemberId) {
        taskService.updatePriority(taskId, request.taskPriorityType(), loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/labels")
    @Operation(summary = "라벨 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "라벨 업데이트 성공")})
    public ResponseEntity<Void> updateLabels(@PathVariable Long taskId,
                                                       @RequestBody TaskRequest.UpdateLabel request,
                                                       @LoginMemberId Long loginMemberId) {
        taskService.updateTaskLabels(taskId, request.labelIds(), loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/project")
    @Operation(summary = "테스크가 속한 프로젝트 이동")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크가 속한 프로젝트 이동 성공")})
    public ResponseEntity<Void> updateProject(@PathVariable Long taskId,
                                                      @RequestBody TaskRequest.UpdateProject request,
                                                      @LoginMemberId Long loginMemberId) {
        taskService.updateProject(taskId, request.projectId(), loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "테스크 삭제")
    @ApiResponses(value={@ApiResponse(responseCode = "204",description = "테스크 삭제 성공")})
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId,
                                           @LoginMemberId Long loginMemberId) {
        taskService.deleteTask(taskId,loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/comments")
    @Operation(summary = "테스크에 속한 댓글 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크에 속한 댓글 조회 성공")})
    public ResponseEntity<List<CommentResponse.Detail>> getComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId));
    }

    @GetMapping("/sidebar-count")
    @Operation(summary = "사이드바 오늘,다음에 해당하는 카운트 수 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "사이드바 오늘,다음에 해당하는 카운트 수 조회 성공")})
    public ResponseEntity<TaskResponse.SidebarTasksCount> getSidebarCount(@LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(taskService.getSidebarTaskCount(loginMemberId));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "테스크 상세 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 상세 조회 성공")})
    public ResponseEntity<TaskResponse.Detail> getTaskById(@PathVariable Long taskId,
                                                    @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId,loginMemberId));
    }


    @GetMapping("/inbox")
    @Operation(summary = "관리함 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "관리함 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.ListSlice> getInboxTasks(@LoginMemberId Long loginMemberId,
                                                                  @ModelAttribute TaskRequest.Cursor cursor,
                                                                  @PageableDefault(size=20) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.INBOX
        );

        return ResponseEntity.ok(taskService.getTasks(searchCondition, cursor, loginMemberId,pageable));
    }

    @GetMapping("/today")
    @Operation(summary = "오늘 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "오늘 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.ListSlice> getTodayTasks(@LoginMemberId Long memberId,
                                                                  @ModelAttribute TaskRequest.Cursor cursor,
                                                                  @PageableDefault(size=20) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.TODAY
        );
        return ResponseEntity.ok(taskService.getTasks(searchCondition,cursor,memberId, pageable));
    }

    @GetMapping("/overdue")
    @Operation(summary = "과거 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "과거 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.ListSlice> getOverdueTasks(@LoginMemberId Long loginMemberId,
                                                                     @ModelAttribute TaskRequest.Cursor cursor,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.OVERDUE
        );

        return ResponseEntity.ok(taskService.getTasks(searchCondition,cursor, loginMemberId, pageable));
    }


    @GetMapping("/upcoming")
    @Operation(summary = "다가오는 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "다가오는 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.ListSlice> getUpcomingTasks(@LoginMemberId Long loginMemberId,
                                                                     @ModelAttribute TaskRequest.Cursor cursor,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.UPCOMING,
                fromDate,
                toDate
        );

        return ResponseEntity.ok(taskService.getTasks(searchCondition,cursor, loginMemberId, pageable));
    }

    @PatchMapping("/{taskId}/due-date")
    @Operation(summary = "만료일 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "만료일 업데이트 성공")})
    public ResponseEntity<Void> updateTaskDueDate(@PathVariable Long taskId,
                                                          @RequestBody TaskRequest.UpdateDueDate request,
                                                          @LoginMemberId Long loginMemberId) {
        taskService.updateTaskDueDate(taskId, request, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/toggle")
    @Operation(summary = "테스크 토글(완료/미완료)")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 토글(완료/미완료) 성공")})
    public ResponseEntity<Void> toggleCompletion(@LoginMemberId Long loginMemberId,
                                                         @PathVariable Long taskId) {
        taskService.toggleCompletion(taskId,loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/upcoming/summary")
    public ResponseEntity<List<TaskResponse.UpcomingDateCount>> getUpcomingSummary(
            @LoginMemberId Long loginMemberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return ResponseEntity.ok(
                taskService.getUpcomingDateCounts(loginMemberId, fromDate, toDate)
        );
    }

}
