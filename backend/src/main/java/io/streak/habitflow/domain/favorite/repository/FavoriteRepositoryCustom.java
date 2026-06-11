package io.streak.habitflow.domain.favorite.repository;

import io.streak.habitflow.domain.favorite.dto.response.FavoriteListResponse;

import java.util.List;

public interface FavoriteRepositoryCustom {
    List<FavoriteListResponse> findByMemberId(Long memberId);
}
