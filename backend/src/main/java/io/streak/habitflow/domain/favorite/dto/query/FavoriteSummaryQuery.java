package io.streak.habitflow.domain.favorite.dto.query;

import io.streak.habitflow.domain.favorite.type.TargetType;

public record FavoriteSummaryQuery(
        Long id,
        Long targetId,
        String targetName,
        TargetType targetType,
        Long targetCount
) {}
