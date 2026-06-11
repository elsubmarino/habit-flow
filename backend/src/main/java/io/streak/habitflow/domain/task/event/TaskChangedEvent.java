package io.streak.habitflow.domain.task.event;

import io.streak.habitflow.domain.task.type.ActivityType;

public record TaskChangedEvent(
        Long taskId,
        Long memberId,
        ActivityType activityType,
        String customMessage
) {
}
