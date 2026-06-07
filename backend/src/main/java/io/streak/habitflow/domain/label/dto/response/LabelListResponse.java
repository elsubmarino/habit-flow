package io.streak.habitflow.domain.label.dto.response;

import io.streak.habitflow.domain.label.entity.Label;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelListResponse {
    private Long id;
    private String name;
    private String color;
    private long sortOrder;

    public static LabelListResponse from(Label label){
        return LabelListResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .sortOrder(label.getSortOrder())
                .color(label.getColor())
                .build();
    }
}
