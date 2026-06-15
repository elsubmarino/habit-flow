package io.streak.habitflow.domain.project.dto.response;

import io.streak.habitflow.domain.project.entity.Project;
import lombok.Builder;

@Builder
public record ProjectListResponse(
        Long id,
        String name,
        String color,
        long taskCount
) {
    public static ProjectListResponse from(Project project) {
        return ProjectListResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .build();
    }
}
