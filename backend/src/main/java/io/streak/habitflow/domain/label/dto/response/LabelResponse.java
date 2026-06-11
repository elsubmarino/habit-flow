package io.streak.habitflow.domain.label.dto.response;

import io.streak.habitflow.domain.label.entity.Label;
import lombok.*;

@Builder
public record LabelResponse(
        Long id,
        String name,
        long sortOrder,
        boolean favorite,
        String color
) {
    public static LabelResponse from(Label label,boolean favorite){
        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .sortOrder(label.getSortOrder())
                .favorite(favorite)
                .color(label.getColor())
                .build();
    }
}
