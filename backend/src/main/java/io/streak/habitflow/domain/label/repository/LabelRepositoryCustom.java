package io.streak.habitflow.domain.label.repository;

import io.streak.habitflow.domain.label.dto.response.LabelResponse;

import java.util.List;

public interface LabelRepositoryCustom {
    List<LabelResponse> searchKeyword(String name, String email);
}
