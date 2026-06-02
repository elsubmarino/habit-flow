package io.streak.habitflow.dto;

import io.streak.habitflow.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private boolean isCompleted;
    private int priority;
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
    private List<TaskAttachmentResponse> taskAttachments = new ArrayList<>();

    @Builder.Default
    private List<LabelResponse> labels = new ArrayList<>();

    public static TaskResponse from(Task task, List<LabelResponse> labelResponses) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .isCompleted(task.isCompleted())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .sortOrder(task.getSortOrder())
                .labels(labelResponses)
                .subTasks(task.getSubTasks().stream()
                        .map(sub -> TaskResponse.from(sub, new ArrayList<>()))
                        .collect(Collectors.toList()))
                .taskAttachments(task.getTaskAttachments().stream()
                        .map(TaskAttachmentResponse::from)
                        .collect(Collectors.toList()));
        if(task.getProject() != null){
            builder.projectId(task.getProject().getId())
                    .projectName(task.getProject().getName())
                    .projectColor(task.getProject().getColor());
        }

        if(task.getUser() != null){
            builder.userId(task.getUser().getId())
                    .userName(task.getUser().getUserName());
        }

        if(task.getParent() != null){
            builder.parentId(task.getParent().getId());
        }


        return builder.build();
    };
}
