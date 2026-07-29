package io.streak.habitflow.domain.favorite.dto.query;

import io.streak.habitflow.global.common.type.TargetType;

import java.util.UUID;

public record FavoriteSummaryQuery(
        Long id,
        UUID publicId,
        Long targetId,
        UUID targetPublicId,
        String targetName,
        TargetType targetType,
        Long targetCount
) {}
