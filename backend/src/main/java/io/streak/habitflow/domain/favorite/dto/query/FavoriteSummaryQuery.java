package io.streak.habitflow.domain.favorite.dto.query;

import io.streak.habitflow.domain.favorite.type.TargetType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FavoriteSummaryQuery {
    private Long id;
    private Long targetId;
    private String targetName;
    private TargetType targetType;
    private long targetCount;
}
