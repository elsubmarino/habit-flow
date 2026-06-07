package io.streak.habitflow.domain.label.repository;

import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;

import java.util.List;

public interface LabelRepositoryCustom {
    List<LabelListResponse> searchKeyword(String name, String email);
}
