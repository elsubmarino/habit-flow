package io.streak.habitflow.domain.activitylog.dto.request;

import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogRequest {
    private Long targetId;
    private TargetType targetType;
    private ActivityType activityType;
    private String customMessage;
}
