package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.dto.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<ProjectResponse> searchKeyword(String keyword, String email);
}
