package io.streak.habitflow.domain.task.event;

import io.streak.habitflow.domain.activitylog.vo.ChangeSet;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;

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
