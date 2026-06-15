package io.streak.habitflow.domain.search.dto.response;


import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record IntegratedSearchResponse(
        List<ProjectListResponse> projects,
        List<TaskResponse> tasks,
        List<LabelListResponse> labels
) {
    public IntegratedSearchResponse{
        if(projects == null){
            projects = new ArrayList<>();
        }
        if(tasks == null){
            tasks = new ArrayList<>();
        }
        if(labels == null){
            labels = new ArrayList<>();
        }
    }
}
