package io.streak.habitflow.domain.favorite.repository;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteListQuery;

import java.util.List;

public interface FavoriteRepositoryCustom {
    List<FavoriteListQuery> findByMemberId(Long memberId);
}
