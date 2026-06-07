package io.streak.habitflow.domain.label.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCreateRequest {
    private String name;
    private String color;
    private boolean isFavorite;
}
