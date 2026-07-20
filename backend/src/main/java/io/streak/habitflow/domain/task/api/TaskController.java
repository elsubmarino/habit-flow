package io.streak.habitflow.domain.task.api;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.service.TaskService;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.global.common.RoutingId;
import io.streak.habitflow.global.common.constant.PageSizeConstants;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import io.streak.habitflow.global.util.HashidsProvider;
import io.streak.habitflow.global.web.LoginMemberId;
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
    private final HashidsProvider hashidsProvider;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "테스크 생성")
    @ApiResponses(value={
            @ApiResponse(responseCode = "201",description = "테스크 생성 성공")
    })
    public ResponseEntity<TaskResponse.Detail> createTask(@LoginMemberId Long loginMemberId,
                                                   @RequestPart(value = "file", required = false) MultipartFile file,
                                                   @RequestPart("taskRequest") @Valid TaskRequest.Create request){
        FileDto fileDto = null;
        if(file!=null&&!file.isEmpty()){
            fileDto = fileStorageService.upload(file);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, fileDto, loginMemberId));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "테스크 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 업데이트 성공")})
    public ResponseEntity<TaskResponse.Detail> updateTask(@PathVariable RoutingId taskId,
                                           @RequestBody @Valid TaskRequest.Update request,
                                           @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(taskService.updateTask(realTaskId, request,loginMemberId));
    }

    @PatchMapping("/{taskId}/priority")
    @Operation(summary = "우선순위 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "우선순위 업데이트 성공")})
    public ResponseEntity<TaskResponse.Detail> updatePriority(@PathVariable RoutingId taskId,
                                                          @RequestBody @Valid TaskRequest.UpdatePriority request,
                                                          @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(taskService.updatePriority(realTaskId, request.taskPriorityType(), loginMemberId));
    }

    @PatchMapping("/{taskId}/labels")
    @Operation(summary = "라벨 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "라벨 업데이트 성공")})
    public ResponseEntity<TaskResponse.Detail> updateTaskLabels(@PathVariable RoutingId taskId,
                                                                @RequestBody @Valid TaskRequest.UpdateLabel request,
                                                                @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(taskService.updateTaskLabels(realTaskId, request.labelIds(), loginMemberId));
    }

    @PatchMapping("/{taskId}/project")
    @Operation(summary = "테스크가 속한 프로젝트 이동")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크가 속한 프로젝트 이동 성공")})
    public ResponseEntity<TaskResponse.Detail> moveTaskToProject(@PathVariable RoutingId taskId,
                                                      @RequestBody @Valid TaskRequest.UpdateProject request,
                                                      @LoginMemberId Long loginMemberId) {
        Long realTaskId = taskId.value();
        Long realProjectId = hashidsProvider.decode(request.projectId());
        return ResponseEntity.ok(taskService.moveTaskToProject(realTaskId, realProjectId, loginMemberId));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "테스크 삭제")
    @ApiResponses(value={@ApiResponse(responseCode = "204",description = "테스크 삭제 성공")})
    public ResponseEntity<Void> deleteTask(@PathVariable RoutingId taskId,
                                           @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        taskService.deleteTask(realTaskId,loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/comments")
    @Operation(summary = "테스크에 속한 댓글 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크에 속한 댓글 조회 성공")})
    public ResponseEntity<List<CommentResponse.Detail>> getComments(@PathVariable RoutingId taskId,
                                                                    @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(commentService.getComments(realTaskId,loginMemberId));
    }

    @GetMapping("/sidebar-count")
    @Operation(summary = "사이드바 오늘,다음에 해당하는 카운트 수 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "사이드바 오늘,다음에 해당하는 카운트 수 조회 성공")})
    public ResponseEntity<TaskResponse.SidebarTasksCount> getSidebarTaskCount(@LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(taskService.getSidebarTaskCount(loginMemberId));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "테스크 상세 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 상세 조회 성공")})
    public ResponseEntity<TaskResponse.Detail> getTaskById(@PathVariable RoutingId taskId,
                                                    @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(taskService.getTaskById(realTaskId,loginMemberId));
    }


    @GetMapping("/inbox")
    @Operation(summary = "관리함 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "관리함 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.SummarySlice> getInboxTasks(@LoginMemberId Long loginMemberId,
                                                                   @ModelAttribute @Valid TaskRequest.Cursor cursor,
                                                                   @PageableDefault(size= PageSizeConstants.CURSOR_PAGING_NORMAL) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.INBOX
        );

        return ResponseEntity.ok(taskService.searchTasks(searchCondition, cursor, loginMemberId,pageable));
    }

    @GetMapping("/today")
    @Operation(summary = "오늘 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "오늘 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.SummarySlice> getTodayTasks(@LoginMemberId Long memberId,
                                                                   @ModelAttribute @Valid TaskRequest.Cursor cursor,
                                                                   @PageableDefault(size=PageSizeConstants.CURSOR_PAGING_NORMAL) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.TODAY
        );
        return ResponseEntity.ok(taskService.searchTasks(searchCondition,cursor,memberId, pageable));
    }

    @GetMapping("/overdue")
    @Operation(summary = "과거 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "과거 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.SummarySlice> getOverdueTasks(@LoginMemberId Long loginMemberId,
                                                                     @ModelAttribute @Valid TaskRequest.Cursor cursor,
                                                                     @PageableDefault(size = PageSizeConstants.CURSOR_PAGING_SMALL) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.OVERDUE
        );

        return ResponseEntity.ok(taskService.searchTasks(searchCondition,cursor, loginMemberId, pageable));
    }


    @GetMapping("/upcoming")
    @Operation(summary = "다가오는 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "다가오는 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.SummarySlice> getUpcomingTasks(@LoginMemberId Long loginMemberId,
                                                                      @ModelAttribute @Valid TaskRequest.Cursor cursor,
                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                                                                      @PageableDefault(size = PageSizeConstants.CURSOR_PAGING_NORMAL) Pageable pageable) {
        TaskRequest.SearchCondition searchCondition = new TaskRequest.SearchCondition(
                TaskFilterType.UPCOMING,
                fromDate,
                toDate
        );

        return ResponseEntity.ok(taskService.searchTasks(searchCondition,cursor, loginMemberId, pageable));
    }

    @PatchMapping("/{taskId}/due-date")
    @Operation(summary = "만료일 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "만료일 업데이트 성공")})
    public ResponseEntity<TaskResponse.Detail> updateTaskDueDate(@PathVariable RoutingId taskId,
                                                          @RequestBody @Valid TaskRequest.UpdateDueDate request,
                                                          @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(taskService.updateTaskDueDate(realTaskId, request, loginMemberId));
    }

    @PatchMapping("/due-date-batch")
    @Operation(summary = "만료일 일괄 업데이트")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "만료일 업데이트 성공")})
    public ResponseEntity<List<TaskResponse.Detail>> updateTaskDueDateBatch(
                                                                 @RequestBody @Valid TaskRequest.UpdateDueDateBatch request,
                                                                 @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(taskService.updateTaskDueDateBatch(request, loginMemberId));
    }

    @PatchMapping("/{taskId}/sort-order")
    @Operation(summary = "정렬순서 변경")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "정렬순서 변경 성공")})
    public ResponseEntity<TaskResponse.Summary> updateSortOrder(@PathVariable RoutingId taskId,
                                                  @RequestBody @Valid TaskRequest.UpdateSortOrder request,
                                                  @LoginMemberId Long loginMemberId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(taskService.updateSortOrder(realTaskId, request, loginMemberId));
    }

    @PatchMapping("/{taskId}/toggle")
    @Operation(summary = "테스크 토글(완료/미완료)")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "테스크 토글(완료/미완료) 성공")})
    public ResponseEntity<TaskResponse.Summary> toggleCompletion(@LoginMemberId Long loginMemberId,
                                                         @PathVariable RoutingId taskId) {
        long realTaskId = taskId.value();
        return ResponseEntity.ok(taskService.toggleCompletion(realTaskId,loginMemberId));
    }

    @GetMapping("/upcoming/summary")
    public ResponseEntity<List<TaskResponse.UpcomingDateCount>> getUpcomingDateCounts(
            @LoginMemberId Long loginMemberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return ResponseEntity.ok(
                taskService.getUpcomingDateCounts(loginMemberId, fromDate, toDate)
        );
    }

    @GetMapping("/labels/{labelId}")
    @Operation(summary = "라벨별 테스크 조회")
    @ApiResponses(value={@ApiResponse(responseCode = "200",description = "라벨별 테스크 조회 성공")})
    public ResponseEntity<TaskResponse.SummarySlice> getTasksByLabel(@LoginMemberId Long loginMemberId,
                                                                     @PathVariable RoutingId labelId,
                                                                     @ModelAttribute @Valid TaskRequest.Cursor cursor,
                                                                     @PageableDefault(size = PageSizeConstants.CURSOR_PAGING_SMALL) Pageable pageable){
        TaskResponse.SummarySlice summarySlice = taskService.getTasksByLabel(labelId.value(),loginMemberId,pageable,cursor);
        return ResponseEntity.ok(summarySlice);
    }
}
