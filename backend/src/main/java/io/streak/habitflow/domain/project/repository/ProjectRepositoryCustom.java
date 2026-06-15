package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.dto.query.ProjectListQuery;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<ProjectListQuery> searchKeyword(String keyword, Long memberId, Pageable pageable);
    List<ProjectListQuery> findByMemberId(Long memberId);
}
