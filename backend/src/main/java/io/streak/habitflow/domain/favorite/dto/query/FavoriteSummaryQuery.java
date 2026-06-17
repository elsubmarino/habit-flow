package io.streak.habitflow.domain.favorite.dto.query;

import io.streak.habitflow.domain.favorite.type.TargetType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FavoriteSummaryQuery {
    private String targetName;
    private Long targetId;
    private TargetType targetType;
    private Long id;
    private long targetCount;
}
