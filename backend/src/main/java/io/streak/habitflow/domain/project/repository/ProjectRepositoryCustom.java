package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<ProjectListResponse> searchKeyword(String keyword, String email);
    List<ProjectListResponse> findByEmail(String email);
}
