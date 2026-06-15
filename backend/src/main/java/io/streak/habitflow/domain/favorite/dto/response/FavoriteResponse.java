package io.streak.habitflow.domain.favorite.dto.response;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteListQuery;
import io.streak.habitflow.domain.favorite.type.TargetType;
import lombok.Builder;

public final class FavoriteResponse{
    @Builder
    public record List(
            String targetName,
            Long targetId,
            TargetType targetType,
            Long id,
            long targetCount
    ) {
        public static List from(FavoriteListQuery favoriteListQuery) {
            return List.builder()
                    .targetName(favoriteListQuery.getTargetName())
                    .targetId(favoriteListQuery.getTargetId())
                    .targetType(favoriteListQuery.getTargetType())
                    .id(favoriteListQuery.getId())
                    .targetCount(favoriteListQuery.getTargetCount())
                    .build();
        }
    }
}

