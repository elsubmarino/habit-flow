package io.streak.habitflow.domain.label.repository;

import io.streak.habitflow.domain.label.dto.query.LabelListQuery;
import io.streak.habitflow.domain.label.entity.Label;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LabelRepositoryCustom {
    List<LabelListQuery> searchKeyword(String name, Long memberId, Pageable pageable);
    List<Label> searchLabelsByCondition(Long labelId, Long memberId, Pageable pageable);
}
