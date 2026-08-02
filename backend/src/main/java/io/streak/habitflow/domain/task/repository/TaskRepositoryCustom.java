package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.dto.query.TaskSearchSummaryQuery;
import io.streak.habitflow.domain.task.dto.query.TaskSummaryQuery;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TaskRepositoryCustom {
    List<TaskSearchSummaryQuery> searchByKeyword(String keyword, Long memberId, Pageable pageable);
    List<TaskSummaryQuery> searchTasksByCondition(TaskRequest.SearchCondition searchCondition, TaskRequest.Cursor cursor, Long memberId, Pageable pageable);
    List<TaskSummaryQuery> searchInboxTasks(TaskRequest.SearchCondition searchCondition, TaskRequest.Cursor cursor, Long memberId, Pageable pageable);
    List<TaskSummaryQuery> findTaskSummariesByProjectPublicId(UUID publicProjectId, Long memberId, Pageable pageable);
    TaskResponse.SidebarTasksCount findSidebarTaskCounts(Long memberId);
    List<TaskResponse.UpcomingDateCount> findUpcomingTaskCountsByDate(Long memberId, LocalDateTime fromDate, LocalDateTime toDate);
    List<TaskSummaryQuery> findTaskSummariesByIds(List<Long> ids);
    boolean existsByIdAndHasAccess(Long taskId, Long memberId);
    boolean existsByPublicIdAndHasAccess(UUID publicId, Long memberId);
    List<TaskSummaryQuery> findTaskSummariesByLabelPublicId(UUID publicLabelId, Pageable pageable, Long loginMemberId);
    Long findMaxSortOrder(Long memberId, Long projectId);
    long countAccessibleTasks(List<Long> taskIds, Long memberId);

}
