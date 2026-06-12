package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.Builder;

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

    public static TaskListResponse of(TaskListQuery taskListQuery, List<LabelListResponse> labelListResponses) {
        return TaskListResponse.builder()
                .id(taskListQuery.getId())
                .name(taskListQuery.getName())
                .description(taskListQuery.getDescription())
                .taskPriorityType(taskListQuery.getTaskPriorityType())
                .dueDate(taskListQuery.getDueDate())
                .projectName(taskListQuery.getProjectName())
                .taskInstanceId(taskListQuery.getTaskInstanceId())
                .countSubTasks(taskListQuery.getCountSubTasks())
                .countSubTasksCompleted(taskListQuery.getCountSubTasksCompleted())
                .countComments(taskListQuery.getCountComments())
                .labels(labelListResponses)
                .build();
    }

    public static TaskListResponse of(TaskMaster taskMaster, List<LabelListResponse> labelListResponses) {
        TaskListResponse.TaskListResponseBuilder builder = TaskListResponse.builder()
                .id(taskMaster.getId())
                .name(taskMaster.getName())
                .taskPriorityType(taskMaster.getTaskPriorityType())
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
