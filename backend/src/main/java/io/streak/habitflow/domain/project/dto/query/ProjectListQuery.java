package io.streak.habitflow.domain.project.dto.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListQuery {
    private Long id;
    private String name;
    private String color;
    private Long taskCount;
}
