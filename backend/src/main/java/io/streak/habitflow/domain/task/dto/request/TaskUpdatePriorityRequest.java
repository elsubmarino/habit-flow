package io.streak.habitflow.domain.task.dto.request;

import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TaskUpdatePriorityRequest {
    private TaskPriorityType taskPriorityType;
}
