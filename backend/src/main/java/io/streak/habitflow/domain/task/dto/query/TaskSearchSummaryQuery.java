package io.streak.habitflow.domain.task.dto.query;

import io.streak.habitflow.domain.task.type.TaskPriorityType;

public record TaskSearchSummaryQuery(
        Long id,
        String name,
        String description,
        TaskPriorityType taskPriorityType,
        long sortOrder,
        String projectName
) {
}
