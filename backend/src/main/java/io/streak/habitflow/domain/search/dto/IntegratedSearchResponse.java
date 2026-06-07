package io.streak.habitflow.domain.search.dto;


import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedSearchResponse {
    private List<ProjectListResponse> projects;
    private List<TaskResponse> tasks;
    private List<LabelListResponse> labels;
}
