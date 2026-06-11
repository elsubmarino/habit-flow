package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.task.entity.TaskInstance;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String name;
    private String description;
    private boolean completed;
    private TaskPriorityType taskPriorityType;
    private LocalDate dueDate;
    private long sortOrder;
    private Long taskInstanceId;

    private Long userId;
    private String userName;

    private Long projectId;
    private String projectName;
    private String projectColor;

    private Long parentId;

    @Builder.Default
    private List<TaskResponse> subTasks = new ArrayList<>();

    @Builder.Default
    private List<LabelListResponse> labels = new ArrayList<>();

    @Builder.Default
    private List<CommentResponse> comments = new ArrayList<>();

    public static TaskResponse from(TaskMaster taskMaster, TaskInstance taskInstance, List<LabelListResponse> labelListResponses) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(taskMaster.getId())
                .name(taskMaster.getName())
                .description(taskMaster.getDescription())
                .completed(taskInstance.isCompleted())
                .taskPriorityType(taskMaster.getTaskPriorityType())
                .dueDate(taskInstance.getDueDate())
                .sortOrder(taskMaster.getSortOrder())
                .labels(labelListResponses)
                .comments(taskMaster.getComments().stream()
                        .map(CommentResponse::from)
                        .toList())
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
