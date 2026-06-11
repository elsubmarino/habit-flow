package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<ProjectListResponse> searchKeyword(String keyword, Long memberId, Pageable pageable);
    List<ProjectListResponse> findByMemberId(Long memberId);
}
