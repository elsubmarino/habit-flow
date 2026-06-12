package io.streak.habitflow.domain.task.dto.request;

import io.streak.habitflow.domain.task.type.TaskFilterType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class TaskSearchCondition {
    private TaskFilterType taskFilterType;
    private Long lastTaskId;
}
