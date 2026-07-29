package io.streak.habitflow.domain.activitylog.dto.request;

import io.streak.habitflow.global.common.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ActivityLogRequest{
    public record Search(
            TargetType targetType,
            List<UUID> targetIds,
            List<UUID> memberIds,
            List<ActivityType> activityType,
            LocalDate fromDate,
            LocalDate toDate
    ){}
}
