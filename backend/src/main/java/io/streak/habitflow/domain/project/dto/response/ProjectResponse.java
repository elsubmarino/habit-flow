package io.streak.habitflow.domain.project.dto.response;

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
    private boolean isFavorite;

    @Builder.Default
    private List<MemberResponse> users = new ArrayList<>();

    public static ProjectResponse from(Project project, boolean isFavorite) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .isFavorite(isFavorite)
                .build();
    }
}
