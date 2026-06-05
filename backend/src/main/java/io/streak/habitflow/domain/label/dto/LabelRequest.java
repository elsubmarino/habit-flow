package io.streak.habitflow.domain.label.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelRequest {
    private Long id;
    private String name;
    private String color;
}
