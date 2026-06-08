package io.streak.habitflow.domain.project.dto.request;

import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdateRequest {
    private String name;
    private String color;
    private Long parentId;
    private AccessType accessType;
    private LayoutType layoutType;
    private boolean favorite;
}
