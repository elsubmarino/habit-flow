package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.task.entity.TaskInstance;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Builder
public record TaskResponse(
        Long id,
        String name,
        String description,
        boolean completed,
        TaskPriorityType taskPriorityType,
        LocalDate dueDate,
        long sortOrder,
        Long taskInstanceId,
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
    public static TaskResponse of(TaskMaster taskMaster, TaskInstance taskInstance, List<LabelListResponse> labelListResponses) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(taskMaster.getId())
                .name(taskMaster.getName())
                .description(taskMaster.getDescription())
                .completed(taskInstance.isCompleted())
                .taskPriorityType(taskMaster.getTaskPriorityType())
                .dueDate(taskInstance.getDueDate())
                .sortOrder(taskMaster.getSortOrder())
                .labels(labelListResponses)
                .recurring(taskMaster.isRecurring())
                .subTasks(taskMaster.getSubTaskMasters().stream()
                        .map(subTaskMaster -> {
                            LocalDate parentDueDate = (taskInstance != null) ? taskInstance.getDueDate() :  null;
                            TaskInstance subTaskInstance = subTaskMaster.getTaskInstances().stream()
                                    .filter(inst ->
                                            Objects.equals(inst.getDueDate(),parentDueDate))
                                    .findFirst()
                                    .orElse(null);
                            return TaskResponse.fromSimpleSubTask(subTaskMaster, subTaskInstance);
                        })
                        .toList());


        if(taskMaster.getProject() != null){
            builder.projectId(taskMaster.getProject().getId())
                    .projectName(taskMaster.getProject().getName())
                    .projectColor(taskMaster.getProject().getColor());
        }else{
            builder.projectId(null)
                    .projectName("관리함")
                    .projectColor("#808080");
        }

        if(taskMaster.getMember() != null){
            builder.userId(taskMaster.getMember().getId());
        }

        if(taskMaster.getParent() != null){
            builder.parentId(taskMaster.getParent().getId());
        }


        return builder.build();
    }

    private static TaskResponse fromSimpleSubTask(TaskMaster subTaskMaster, TaskInstance taskInstance){
        return TaskResponse.builder()
                .id(subTaskMaster.getId())
                .name(subTaskMaster.getName())
                .taskInstanceId(taskInstance != null ? taskInstance.getId() : null)
                .completed(taskInstance != null && taskInstance.isCompleted())
                .build();
    }
}
