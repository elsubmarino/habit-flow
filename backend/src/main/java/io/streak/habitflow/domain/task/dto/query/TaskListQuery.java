package io.streak.habitflow.domain.task.dto.query;

import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class TaskListQuery {
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
    private boolean timeSpecified;
}
