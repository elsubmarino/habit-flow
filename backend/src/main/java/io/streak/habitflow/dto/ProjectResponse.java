package io.streak.habitflow.dto;

import io.streak.habitflow.entity.Project;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String color;
    private long sortOrder;

    private String userId;
    private String userName;

    public static ProjectResponse from(Project project) {
        ProjectResponseBuilder builder = ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .sortOrder(project.getSortOrder());

        if(project.getUser() != null){
            builder.userId(project.getUser().getUserId())
                    .userName(project.getUser().getUserName());
        }

        return builder.build();
    }
}
