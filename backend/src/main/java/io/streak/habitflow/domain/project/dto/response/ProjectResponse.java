package io.streak.habitflow.domain.project.dto.response;

import io.streak.habitflow.domain.project.entity.Project;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String color;
    private boolean isFavorite;

    public static ProjectResponse from(Project project, boolean isFavorite) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .isFavorite(isFavorite)
                .build();
    }
}
