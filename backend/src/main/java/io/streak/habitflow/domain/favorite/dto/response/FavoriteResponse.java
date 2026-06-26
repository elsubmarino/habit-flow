package io.streak.habitflow.domain.favorite.dto.response;

import io.streak.habitflow.domain.favorite.dto.query.FavoriteSummaryQuery;
import io.streak.habitflow.global.common.type.TargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public final class FavoriteResponse{
    @Builder
    public record Summary(
            @Schema(description = "대상의 이름 (라벨명,프로젝트명)",examples = {"라벨명","프로젝트명"})
            String targetName,

            @Schema(description = "대상 아이디 (라벨아이디,프로젝트아이디)",examples = {"6q9WeDv5"})
            String targetId,

            @Schema(description = "대상 타입",examples = {"PROJECT","LABEL"})
            TargetType targetType,

            @Schema(description = "즐겨찾기 아이디",example = "6q9WeDv5")
            String id,

            @Schema(description = "즐겨찾기에 해당하는 테스크의 수")
            long targetCount
    ) {
        public static Summary to(FavoriteSummaryQuery favoriteSummaryQuery,String encodedTargetId, String encodedId) {
            return Summary.builder()
                    .targetName(favoriteSummaryQuery.targetName())
                    .targetId(encodedTargetId)
                    .targetType(favoriteSummaryQuery.targetType())
                    .id(encodedId)
                    .targetCount(favoriteSummaryQuery.targetCount())
                    .build();
        }
    }
}

