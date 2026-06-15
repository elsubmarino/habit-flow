package io.streak.habitflow.domain.search.dto.response;

import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

public final class IntegratedResponse {
    @Builder
    public record Search(
            List<ProjectResponse.List> projects,
            List<TaskResponse> tasks,
            List<LabelResponse.List> labels
    ){
        public Search{
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
}
