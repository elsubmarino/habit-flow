package io.streak.habitflow.domain.task.dto.response;

import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.PriorityType;
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
public class TaskListResponse {
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
    private List<TaskListResponse> subTasks = new ArrayList<>();

    @Builder.Default
    private List<LabelListResponse> labels = new ArrayList<>();

    @Builder.Default
    private List<CommentResponse> comments = new ArrayList<>();

    public static TaskListResponse from(Task task, List<LabelListResponse> labelListResponses) {
        TaskListResponse.TaskListResponseBuilder builder = TaskListResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .isCompleted(task.isCompleted())
                .priorityType(task.getPriorityType())
                .dueDate(task.getDueDate())
                .sortOrder(task.getSortOrder())
                .labels(labelListResponses);


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
    }
}
