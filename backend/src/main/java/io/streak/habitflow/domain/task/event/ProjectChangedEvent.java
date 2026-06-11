package io.streak.habitflow.domain.task.event;

import io.streak.habitflow.domain.task.type.ActivityType;

public record ProjectChangedEvent(
        Long projectId,
        Long memberId,
        ActivityType activityType,
        String customMessage
) {
}
