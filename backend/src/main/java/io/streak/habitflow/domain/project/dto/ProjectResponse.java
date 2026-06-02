package io.streak.habitflow.domain.project.dto;

import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.user.dto.UserResponse;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String color;
    private long sortOrder;

    @Builder.Default
    private List<UserResponse> users = new ArrayList<>();

    public static ProjectResponse from(Project project, List<UserResponse> userResponses) {
        ProjectResponseBuilder builder = ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .users(userResponses)
                .sortOrder(project.getSortOrder());

        return builder.build();
    }
}
