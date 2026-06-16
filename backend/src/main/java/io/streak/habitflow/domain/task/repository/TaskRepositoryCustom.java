package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.dto.query.TaskListQuery;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepositoryCustom {
    List<Task> searchTasks(TaskRequest.Update request, String email);
    Optional<Task> searchTaskInfo(Long taskId);
    List<TaskResponse> searchKeyword(String keyword, Long memberId, Pageable pageable);
    List<TaskListQuery> searchTasksByCondition(TaskRequest.SearchCondition searchCondition, Long memberId, Pageable pageable);
    List<TaskListQuery> findTasksByProject(Long projectId, Long memberId, Pageable pageable);
    long countTasksByCondition(TaskFilterType taskFilterType, Long memberId);
    Optional<Task> findByIdWithProject(Long taskId);
    List<TaskResponse.UpcomingDateCount>countUpcomingTasksByDate(Long memberId, LocalDateTime fromDate, LocalDateTime toDate);
}
