package io.streak.habitflow.domain.task.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TaskUpdateDueDateRequest {
    private LocalDate dueDate;
    private boolean recurring;
    private String recurrenceRule;
    private int recurrenceInterval;
    private String recurrenceDays;
    private Integer recurrenceDayOfMonth;
}
