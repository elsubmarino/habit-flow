package io.streak.habitflow.domain.task.dto;

import io.streak.habitflow.domain.task.entity.TaskHistory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskHistoryResponse {
    private Long id;
    private String description;
    private long sortOrder;

    private Long taskId;

    public static TaskHistoryResponse from(TaskHistory taskHistory){
        TaskHistoryResponseBuilder builder =  TaskHistoryResponse.builder()
                .id(taskHistory.getId())
                .description(taskHistory.getDescription())
                .sortOrder(taskHistory.getSortOrder());

        if(taskHistory.getTask() != null){
            builder.taskId(taskHistory.getTask().getId());
        }
        return builder.build();
    }

}
