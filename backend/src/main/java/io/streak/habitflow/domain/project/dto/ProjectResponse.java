package io.streak.habitflow.domain.project.dto;

import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.member.dto.MemberResponse;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String color;
    private long sortOrder;

    @Builder.Default
    private List<MemberResponse> users = new ArrayList<>();

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .sortOrder(project.getSortOrder())
                .build();
    }
}
