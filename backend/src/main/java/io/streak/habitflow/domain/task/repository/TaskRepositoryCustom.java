package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.entity.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepositoryCustom {
    List<Task> searchTasks(TaskRequest taskRequest);
    Optional<Task> searchTaskInfo(Long id);
}
