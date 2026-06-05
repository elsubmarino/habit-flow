package io.streak.habitflow.domain.project.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {
    private String name;
    private String color;
}
