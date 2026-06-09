package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskFilterType;

import java.util.List;
import java.util.Optional;

public interface TaskRepositoryCustom {
    List<Task> searchTasks(TaskUpdateRequest taskUpdateRequest, String email);
    Optional<Task> searchTaskInfo(Long taskId);
    List<TaskResponse> searchKeyword(String keyword, String email);
    List<Task> searchTasksByCondition(TaskSearchCondition taskSearchCondition, Long memberId);
    long countTasksByCondition(TaskFilterType taskFilterType, String email);
}
