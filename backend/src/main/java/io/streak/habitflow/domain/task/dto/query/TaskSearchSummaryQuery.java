package io.streak.habitflow.domain.task.dto.query;

import io.streak.habitflow.domain.task.type.TaskPriorityType;

import java.util.UUID;

public record TaskSearchSummaryQuery(
        Long id,
        UUID publicId,
        String name,
        String description,
        TaskPriorityType taskPriorityType,
        long sortOrder,
        String projectName
) {
}
