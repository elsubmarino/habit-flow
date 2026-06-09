package io.streak.habitflow.domain.task.event;

import io.streak.habitflow.domain.task.type.ActivityType;

public record TaskCompletedEvent(
        Long taskId,
        String email,
        ActivityType activityType
) {
}
