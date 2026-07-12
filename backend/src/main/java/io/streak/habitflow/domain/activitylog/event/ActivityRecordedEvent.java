package io.streak.habitflow.domain.activitylog.event;

import io.streak.habitflow.domain.activitylog.vo.ChangeSet;
import io.streak.habitflow.global.common.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;

import java.util.List;

public record ActivityRecordedEvent(
        Long targetId,
        Long memberId,
        TargetType targetType,
        ActivityType activityType,
        String targetName,
        List<ChangeSet> changes
) {
}
