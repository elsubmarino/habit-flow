package io.streak.habitflow.domain.label.repository;

import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.entity.Label;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LabelRepositoryCustom {
    List<LabelListResponse> searchKeyword(String name, Long memberId, Pageable pageable);
    List<Label> searchLabelsByCondition(Long labelId, Long memberId, Pageable pageable);
}
