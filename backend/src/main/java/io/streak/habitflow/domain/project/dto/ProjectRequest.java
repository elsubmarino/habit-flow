package io.streak.habitflow.domain.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectRequest {
    private String name;
    private String color;
}
