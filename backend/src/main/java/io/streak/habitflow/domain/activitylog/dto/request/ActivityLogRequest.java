package io.streak.habitflow.domain.activitylog.dto.request;

import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public final class ActivityLogRequest{
    @Builder
    public record Create(
            Long targetId,
            TargetType targetType,
            ActivityType activityType,
            String customMessage){}
    public record SearchCondition(
            List<Long> projectIds,
            List<Long> memberIds,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate targetDate
    ){}
}
