package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.entity.TaskInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskInstanceRepository extends JpaRepository<TaskInstance, Long> {
}
