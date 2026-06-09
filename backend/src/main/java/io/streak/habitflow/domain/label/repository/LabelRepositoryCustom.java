package io.streak.habitflow.domain.label.repository;

import io.streak.habitflow.domain.label.dto.request.LabelSearchRequest;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;

import java.util.List;

public interface LabelRepositoryCustom {
    List<LabelListResponse> searchKeyword(String name, String email);
    List<Label> searchLabelsByCondition(Long labelId, Long memberId);
}
