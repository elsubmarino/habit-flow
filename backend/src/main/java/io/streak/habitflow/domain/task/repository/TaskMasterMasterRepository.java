package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.task.entity.TaskMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskMasterMasterRepository extends JpaRepository<TaskMaster,Long>, TaskMasterRepositoryCustom {
    List<TaskMaster> findByProjectId(Long projectId);
}
