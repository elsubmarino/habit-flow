package io.streak.habitflow.domain.label.dto.response;

import io.streak.habitflow.domain.label.entity.Label;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponse {
    private Long id;
    private String name;
    private long sortOrder;
    private boolean favorite;
    private String color;

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
