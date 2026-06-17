package io.streak.habitflow.domain.favorite.dto.response;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteSummaryQuery;
import io.streak.habitflow.domain.favorite.type.TargetType;
import lombok.Builder;

public final class FavoriteResponse{
    @Builder
    public record Summary(
            String targetName,
            Long targetId,
            TargetType targetType,
            Long id,
            long targetCount
    ) {
        public static Summary from(FavoriteSummaryQuery favoriteSummaryQuery) {
            return Summary.builder()
                    .targetName(favoriteSummaryQuery.getTargetName())
                    .targetId(favoriteSummaryQuery.getTargetId())
                    .targetType(favoriteSummaryQuery.getTargetType())
                    .id(favoriteSummaryQuery.getId())
                    .targetCount(favoriteSummaryQuery.getTargetCount())
                    .build();
        }
    }
}

