package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private LocalDateTime dueDate;
    private long sortOrder;

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

    public static TaskResponse from(Task task, List<LabelListResponse> labelListResponses) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .taskPriorityType(task.getTaskPriorityType())
                .dueDate(task.getDueDate())
                .sortOrder(task.getSortOrder())
                .labels(labelListResponses)
                .comments(task.getComments().stream()
                        .map(CommentResponse::from)
                        .toList())
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
