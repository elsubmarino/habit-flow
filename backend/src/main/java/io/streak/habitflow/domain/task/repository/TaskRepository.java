package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task,Long>, TaskRepositoryCustom {
    List<Task> findByProjectId(Long projectId);
}
