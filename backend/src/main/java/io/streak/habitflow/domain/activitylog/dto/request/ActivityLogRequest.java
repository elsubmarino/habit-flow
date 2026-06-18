package io.streak.habitflow.domain.activitylog.dto.request;

import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import lombok.Builder;

public final class ActivityLogRequest{
    @Builder
    public record Create(
            Long targetId,
            TargetType targetType,
            ActivityType activityType,
            String targetName){}
}
