package io.streak.habitflow.domain.label.repository;

import io.streak.habitflow.domain.label.dto.query.LabelSummaryQuery;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface LabelRepositoryCustom {
    List<LabelSummaryQuery> searchKeyword(String name, Long memberId, Pageable pageable);
    List<Label> searchLabelsByCondition(Long labelId, Long memberId, Pageable pageable);
    Map<Long, List<LabelResponse.Summary>> findLabelsByTaskIds(List<Long> taskIds);
    List<LabelResponse.Summary> findByTask(Long taskId);
}
