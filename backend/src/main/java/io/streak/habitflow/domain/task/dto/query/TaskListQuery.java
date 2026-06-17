package io.streak.habitflow.domain.task.dto.query;

import io.streak.habitflow.domain.task.type.TaskPriorityType;

import java.time.LocalDateTime;

public record TaskListQuery (
    Long id,
    String name,
    String description,
    TaskPriorityType taskPriorityType,
    LocalDateTime dueDate,
    long sortOrder,
    String projectName,
    long countSubTasks,
    long countSubTasksCompleted,
    long countComments,
    boolean timeSpecified
){}
