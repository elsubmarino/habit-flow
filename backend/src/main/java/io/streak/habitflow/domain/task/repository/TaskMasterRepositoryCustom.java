package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListQuery;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TaskMasterRepositoryCustom {
    List<TaskMaster> searchTasks(TaskUpdateRequest taskUpdateRequest, String email);
    Optional<TaskMaster> searchTaskInfo(Long taskId);
    List<TaskResponse> searchKeyword(String keyword, Long memberId, Pageable pageable);
    List<TaskListQuery> searchTasksByCondition(TaskSearchCondition taskSearchCondition, Long memberId, Pageable pageable);
    long countTasksByCondition(TaskFilterType taskFilterType, Long memberId);
}
