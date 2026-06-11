package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
public record TaskListResponse(
        Long id,
        String name,
        String description,
        TaskPriorityType taskPriorityType,
        LocalDate dueDate,
        long sortOrder,
        String projectName,
        long countSubTasks,
        long countSubTasksCompleted,
        long countComments,
        Long taskInstanceId,
        List<LabelListResponse> labels
) {
    public TaskListResponse{
        if(labels== null){
            labels = new ArrayList<>();
        }
    }
    public static TaskListResponse from(TaskMaster taskMaster, List<LabelListResponse> labelListResponses) {
        TaskListResponse.TaskListResponseBuilder builder = TaskListResponse.builder()
                .id(taskMaster.getId())
                .name(taskMaster.getName())
                .taskPriorityType(taskMaster.getTaskPriorityType())
                //TODO
//                .dueDate(taskMaster.getDueDate())
                .sortOrder(taskMaster.getSortOrder())
                .labels(labelListResponses);
        if(taskMaster.getProject() != null){
            builder.projectName(taskMaster.getProject().getName());
        }else{
            builder.projectName("관리함");
        }

        return builder.build();
    }
}
