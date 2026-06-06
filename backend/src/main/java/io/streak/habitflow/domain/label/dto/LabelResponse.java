package io.streak.habitflow.domain.label.dto;

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

    private String userId;
    private String userName;

    public static LabelResponse from(Label label){
        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .sortOrder(label.getSortOrder())
                .build();
    }
}
