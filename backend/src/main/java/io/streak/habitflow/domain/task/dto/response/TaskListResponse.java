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
public class TaskListResponse {
    private Long id;
    private String name;
    private String description;
    private TaskPriorityType taskPriorityType;
    private LocalDateTime dueDate;
    private long sortOrder;
    private String projectName;
    private long countSubTasks;
    private long countSubTasksCompleted;
    private long countComments;

    @Builder.Default
    private List<LabelListResponse> labels = new ArrayList<>();


    public static TaskListResponse from(Task task, List<LabelListResponse> labelListResponses) {
        TaskListResponse.TaskListResponseBuilder builder = TaskListResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .taskPriorityType(task.getTaskPriorityType())
                .dueDate(task.getDueDate())
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
