package io.streak.habitflow.domain.label.dto.response;

import io.streak.habitflow.domain.label.entity.Label;
import lombok.Builder;

@Builder
public record LabelListResponse(
        Long id,
        String name,
        String color,
         long sortOrder
) {
    public static LabelListResponse from(Label label){
        return LabelListResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .sortOrder(label.getSortOrder())
                .color(label.getColor())
                .build();
    }
}