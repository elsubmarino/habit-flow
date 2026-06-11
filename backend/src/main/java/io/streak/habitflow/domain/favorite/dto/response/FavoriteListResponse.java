package io.streak.habitflow.domain.favorite.dto.response;

import io.streak.habitflow.domain.favorite.type.TargetType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public record FavoriteListResponse(
        String targetName,
        Long targetId,
        TargetType targetType,
        Long id,
        long targetCount
) {}
