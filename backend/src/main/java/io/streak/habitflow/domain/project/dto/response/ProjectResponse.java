package io.streak.habitflow.domain.project.dto.response;

import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import lombok.Builder;

@Builder
public record ProjectResponse(
        Long id,
        String name,
        String color,
        boolean favorite,
        Long parentId,
        String parentName,
        AccessType accessType,
        LayoutType layoutType
) {
    public static ProjectResponse of(Project project, boolean favorite) {
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
