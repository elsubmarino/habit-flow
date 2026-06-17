package io.streak.habitflow.domain.label.dto.response;

import io.streak.habitflow.domain.label.dto.query.LabelListQuery;
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
    public record List(
            Long id,
            String name,
            String color,
            long sortOrder
    ){
        public static List from(Label label){
            return List.builder()
                    .id(label.getId())
                    .name(label.getName())
                    .sortOrder(label.getSortOrder())
                    .color(label.getColor())
                    .build();
        }

        public static List from(LabelListQuery  labelListQuery){
            return List.builder()
                    .id(labelListQuery.id())
                    .name(labelListQuery.name())
                    .sortOrder(labelListQuery.sortOrder())
                    .color(labelListQuery.color())
                    .build();
        }
    }
}
