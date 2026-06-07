package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.entity.ProjectUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectUserRepository extends JpaRepository<ProjectUser, Long> {
    void deleteByProjectId(Long id);
}
