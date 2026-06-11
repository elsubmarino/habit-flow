package io.streak.habitflow.domain.favorite.dto.response;

import io.streak.habitflow.domain.favorite.type.TargetType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteListResponse {
    private String targetName;
    private Long targetId;
    private TargetType targetType;
    private Long id;
    private long targetCount;
}
