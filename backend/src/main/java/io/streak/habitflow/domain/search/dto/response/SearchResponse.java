package io.streak.habitflow.domain.search.dto.response;

import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SearchResponse {
    @Builder
    public record SearchResult(
            @Schema(description = "검색된 프로젝트의 리스트")
            List<ProjectResponse.Summary> projects,

            @Schema(description = "검색된 테스크의 리스트")
            List<TaskResponse.Summary> tasks,

            @Schema(description = "검색된 라벨의 리스트")
            List<LabelResponse.Summary> labels
    ){
        public SearchResult {
            projects = Objects.requireNonNullElse(projects, new ArrayList<>());
            tasks = Objects.requireNonNullElse(tasks, new ArrayList<>());
            labels = Objects.requireNonNullElse(labels, new ArrayList<>());
        }
    }
}
