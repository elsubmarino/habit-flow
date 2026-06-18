package io.streak.habitflow.domain.task.event;

import io.streak.habitflow.domain.activitylog.dto.ChangeSet;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;

import java.util.List;

public record TaskChangedEvent(
        Long targetId,
        Long memberId,
        TargetType targetType,
        ActivityType activityType,
        String targetName,
        List<ChangeSet> changes
) {
}
