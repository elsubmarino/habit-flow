package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.entity.TaskInstance;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskInstanceRepository extends JpaRepository<TaskInstance, Long> {
    Optional<TaskInstance> findByTaskMasterAndIsCompletedFalse(TaskMaster taskMaster);
    Optional<TaskInstance> findTopByTaskMasterOrderByTargetDateDesc(TaskMaster taskMaster);
}
