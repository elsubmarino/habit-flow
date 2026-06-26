package io.streak.habitflow.domain.activitylog.dto.request;

import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;

import java.time.LocalDate;
import java.util.List;

public final class ActivityLogRequest{
    public record Search(
            TargetType targetType,
            List<String> targetIds,
            List<String> memberIds,
            List<ActivityType> activityType,
            LocalDate fromDate,
            LocalDate toDate
    ){}
}
