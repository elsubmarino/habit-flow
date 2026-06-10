package io.streak.habitflow.domain.task.dto.request;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TaskUpdateDueDateRequest {
    private LocalDateTime dueDate;
}
