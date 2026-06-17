package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.dto.query.ProjectSummaryQuery;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<ProjectSummaryQuery> searchKeyword(String keyword, Long memberId, Pageable pageable);
    List<ProjectSummaryQuery> findByMemberId(Long memberId);
}
