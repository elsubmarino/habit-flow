package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<ProjectListResponse> searchKeyword(String keyword, Long memberId);
    List<ProjectListResponse> findByMemberId(Long memberId);
}
