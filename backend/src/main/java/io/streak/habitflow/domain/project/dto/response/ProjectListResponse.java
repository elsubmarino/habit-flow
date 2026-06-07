package io.streak.habitflow.domain.project.dto.response;

import io.streak.habitflow.domain.member.dto.MemberResponse;
import io.streak.habitflow.domain.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListResponse {
    private Long id;
    private String name;
    private String color;

    public static ProjectListResponse from(Project project) {
        return ProjectListResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .build();
    }
}
