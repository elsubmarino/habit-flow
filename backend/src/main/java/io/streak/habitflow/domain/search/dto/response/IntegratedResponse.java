package io.streak.habitflow.domain.search.dto.response;

import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class IntegratedResponse {
    @Builder
    public record Search(
            List<ProjectResponse.Summary> projects,
            List<TaskResponse> tasks,
            List<LabelResponse.Summary> labels
    ){
        public Search{
            projects = Objects.requireNonNullElse(projects, new ArrayList<>());
            tasks = Objects.requireNonNullElse(tasks, new ArrayList<>());
            labels = Objects.requireNonNullElse(labels, new ArrayList<>());
        }
    }
}
