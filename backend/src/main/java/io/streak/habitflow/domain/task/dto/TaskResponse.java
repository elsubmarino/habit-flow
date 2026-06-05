package io.streak.habitflow.domain.task.dto;

import io.streak.habitflow.domain.attachment.dto.AttachmentResponse;
import io.streak.habitflow.domain.label.dto.LabelResponse;
import io.streak.habitflow.domain.task.entity.PriorityType;
import io.streak.habitflow.domain.task.entity.Task;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private boolean isCompleted;
    private PriorityType priorityType;
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
    private List<AttachmentResponse> taskAttachments = new ArrayList<>();

    @Builder.Default
    private List<LabelResponse> labels = new ArrayList<>();

    public static TaskResponse from(Task task, List<LabelResponse> labelResponses) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .isCompleted(task.isCompleted())
                .priorityType(task.getPriorityType())
                .dueDate(task.getDueDate())
                .sortOrder(task.getSortOrder())
                .labels(labelResponses)
                .subTasks(task.getSubTasks().stream()
                        .map(sub -> TaskResponse.from(sub, new ArrayList<>()))
                        .collect(Collectors.toList()));
        if(task.getProject() != null){
            builder.projectId(task.getProject().getId())
                    .projectName(task.getProject().getName())
                    .projectColor(task.getProject().getColor());
        }

        if(task.getMember() != null){
            builder.userId(task.getMember().getId());
        }

        if(task.getParent() != null){
            builder.parentId(task.getParent().getId());
        }


        return builder.build();
    };
}
