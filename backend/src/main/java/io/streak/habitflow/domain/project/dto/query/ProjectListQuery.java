package io.streak.habitflow.domain.project.dto.query;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectListQuery {
    private Long id;
    private String name;
    private String color;
    private long taskCount;
}
