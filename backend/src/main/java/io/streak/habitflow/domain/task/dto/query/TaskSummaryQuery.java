package io.streak.habitflow.domain.task.dto.query;

import io.streak.habitflow.domain.task.type.TaskPriorityType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskSummaryQuery(
    Long id,
    UUID publicId,
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
