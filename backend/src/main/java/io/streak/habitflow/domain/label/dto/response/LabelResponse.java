package io.streak.habitflow.domain.label.dto.response;

import io.streak.habitflow.domain.label.dto.query.LabelSummaryQuery;
import io.streak.habitflow.domain.label.entity.Label;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public final class LabelResponse{
    @Builder
    public record Detail(
            @Schema(description = "라벨 ID",example = "1")
            Long id,

            @Schema(description = "라벨명")
            String name,

            @Schema(description = "정렬순서")
            long sortOrder,

            @Schema(description = "즐겨찾기 여부")
            boolean favorite,

            @Schema(description = "즐겨찾기 색상(헥사코드)",example = "#123456")
            String color
    ){
        public static Detail of(Label label, boolean favorite){
            return Detail.builder()
                    .id(label.getId())
                    .name(label.getName())
                    .sortOrder(label.getSortOrder())
                    .favorite(favorite)
                    .color(label.getColor())
                    .build();
        }
    }

    @Builder
    public record Summary(
            @Schema(description = "라벨 ID",example = "1")
            Long id,
            @Schema(description = "라벨명")
            String name,
            @Schema(description = "즐겨찾기 색상(헥사코드)",example = "#123456")
            String color,
            @Schema(description = "정렬순서")
            long sortOrder
    ){
        public static Summary from(Label label){
            return Summary.builder()
                    .id(label.getId())
                    .name(label.getName())
                    .sortOrder(label.getSortOrder())
                    .color(label.getColor())
                    .build();
        }

        public static Summary from(LabelSummaryQuery labelSummaryQuery){
            return Summary.builder()
                    .id(labelSummaryQuery.id())
                    .name(labelSummaryQuery.name())
                    .sortOrder(labelSummaryQuery.sortOrder())
                    .color(labelSummaryQuery.color())
                    .build();
        }
    }
}
