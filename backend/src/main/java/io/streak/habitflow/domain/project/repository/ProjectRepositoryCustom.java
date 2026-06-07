package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<ProjectResponse> searchKeyword(String keyword, String email);
}
