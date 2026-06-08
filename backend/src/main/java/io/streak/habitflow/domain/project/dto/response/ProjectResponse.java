package io.streak.habitflow.domain.project.dto.response;

import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String color;
    private boolean favorite;

    private Long parentId;
    private String parentName;

    private AccessType accessType;
    private LayoutType layoutType;

    public static ProjectResponse from(Project project, boolean favorite) {
        ProjectResponseBuilder builder = ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .layoutType(project.getLayoutType())
                .accessType(project.getAccessType())
                .favorite(favorite);
        if(project.getParent() != null){
            builder.parentId(project.getParent().getId());
            builder.parentName(project.getParent().getName());
        }
        return builder.build();
    }
}
