package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
public record TaskResponse(
        Long id,
        String name,
        String description,
        boolean completed,
        TaskPriorityType taskPriorityType,
        LocalDate dueDate,
        long sortOrder,
        Long userId,
        String userName,
        Long projectId,
        String projectName,
        String projectColor,
        Long parentId,
        boolean recurring,
        List<TaskResponse> subTasks,
        List<LabelListResponse> labels,
        List<CommentResponse> comments
) {
    public TaskResponse{
        if(subTasks == null){
            subTasks = new ArrayList<>();
        }
        if(labels == null){
            labels = new ArrayList<>();
        }
        if(comments == null){
            comments = new ArrayList<>();
        }
    }
    public static TaskResponse of(Task task, List<LabelListResponse> labelListResponses) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .taskPriorityType(task.getTaskPriorityType())
                .dueDate(task.getDueDate())
                .sortOrder(task.getSortOrder())
                .labels(labelListResponses)
                .recurring(task.isRecurring())
                .subTasks(task.getSubTasks().stream()
                        .map(TaskResponse::fromSimpleSubTask)
                        .toList());


        if(task.getProject() != null){
            builder.projectId(task.getProject().getId())
                    .projectName(task.getProject().getName())
                    .projectColor(task.getProject().getColor());
        }else{
            builder.projectId(null)
                    .projectName("관리함")
                    .projectColor("#808080");
        }

        if(task.getMember() != null){
            builder.userId(task.getMember().getId());
        }

        if(task.getParent() != null){
            builder.parentId(task.getParent().getId());
        }


        return builder.build();
    }

    private static TaskResponse fromSimpleSubTask(Task subTask){
        return TaskResponse.builder()
                .id(subTask.getId())
                .name(subTask.getName())
                .completed(subTask.isCompleted())
                .build();
    }
}
