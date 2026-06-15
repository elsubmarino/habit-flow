package io.streak.habitflow.domain.label.dto.query;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LabelListQuery {
    private Long id;
    private String name;
    private long sortOrder;
    private boolean favorite;
    private String color;
}
