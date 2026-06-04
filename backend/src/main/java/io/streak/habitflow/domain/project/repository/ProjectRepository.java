package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
