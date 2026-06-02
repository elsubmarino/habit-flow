package io.streak.habitflow.dto;

import io.streak.habitflow.entity.TaskHistory;
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
