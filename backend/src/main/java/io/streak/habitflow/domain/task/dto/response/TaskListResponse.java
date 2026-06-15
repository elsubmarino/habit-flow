package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.task.dto.query.TaskListQuery;
import io.streak.habitflow.domain.task.entity.Task;
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
        List<LabelResponse.List> labels
) {
    public TaskListResponse{
        if(labels== null){
            labels = new ArrayList<>();
        }
    }

    public static TaskListResponse of(TaskListQuery taskListQuery, List<LabelResponse.List> labelListResponses) {
        return TaskListResponse.builder()
                .id(taskListQuery.getId())
                .name(taskListQuery.getName())
                .description(taskListQuery.getDescription())
                .taskPriorityType(taskListQuery.getTaskPriorityType())
                .dueDate(taskListQuery.getDueDate())
                .projectName(taskListQuery.getProjectName())
                .countSubTasks(taskListQuery.getCountSubTasks())
                .countSubTasksCompleted(taskListQuery.getCountSubTasksCompleted())
                .countComments(taskListQuery.getCountComments())
                .labels(labelListResponses)
                .build();
    }

    public static TaskListResponse of(Task task, List<LabelResponse.List> labelListResponses) {
        TaskListResponse.TaskListResponseBuilder builder = TaskListResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .taskPriorityType(task.getTaskPriorityType())
                .sortOrder(task.getSortOrder())
                .labels(labelListResponses);
        if(task.getProject() != null){
            builder.projectName(task.getProject().getName());
        }else{
            builder.projectName("관리함");
        }

        return builder.build();
    }
}
