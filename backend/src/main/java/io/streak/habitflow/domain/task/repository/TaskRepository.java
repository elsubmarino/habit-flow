package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task,Long>, TaskRepositoryCustom {
}
