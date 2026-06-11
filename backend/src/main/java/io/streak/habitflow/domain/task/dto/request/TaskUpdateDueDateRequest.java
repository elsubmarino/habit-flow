package io.streak.habitflow.domain.task.dto.request;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class TaskUpdateDueDateRequest {
    private LocalDate dueDate;
    private boolean isRecurring;
    private String recurrenceRule;
    private int recurrenceInterval;
    private String recurrenceDays;
    private Integer recurrenceDayOfMonth;
}
