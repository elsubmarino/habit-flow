package io.streak.habitflow.domain.favorite.dto.response;

import io.streak.habitflow.domain.favorite.type.TargetType;

public record FavoriteListResponse(
        String targetName,
        Long targetId,
        TargetType targetType,
        Long id,
        long targetCount
) {}
