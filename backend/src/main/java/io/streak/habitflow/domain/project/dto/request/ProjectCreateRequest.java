package io.streak.habitflow.domain.project.dto.request;

import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProjectCreateRequest {
    private String name;
    private String color;
    private Long parentId;
    private AccessType accessType;
    private boolean favorite;
    private LayoutType layoutType;

}
