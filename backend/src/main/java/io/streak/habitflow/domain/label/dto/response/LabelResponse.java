package io.streak.habitflow.domain.label.dto.response;

import io.streak.habitflow.domain.label.dto.query.LabelSummaryQuery;
import io.streak.habitflow.domain.label.entity.Label;
import lombok.Builder;

public final class LabelResponse{
    @Builder
    public record Detail(
            Long id,
            String name,
            long sortOrder,
            boolean favorite,
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
            Long id,
            String name,
            String color,
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
