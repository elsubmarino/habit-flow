package io.streak.habitflow.domain.favorite.repository;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteSummaryQuery;

import java.util.List;

public interface FavoriteRepositoryCustom {
    List<FavoriteSummaryQuery> findFavoritesByMemberId(Long memberId);
}
