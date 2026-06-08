package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task,Long>, TaskRepositoryCustom {
    List<Task> findByProjectId(Long projectId);
}
