package io.streak.habitflow.domain.task.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskHistoryRequest {
    private String description;

    private Long taskId;
}
