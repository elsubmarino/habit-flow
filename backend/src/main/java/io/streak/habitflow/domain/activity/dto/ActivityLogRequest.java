package io.streak.habitflow.domain.activity.dto;

import io.streak.habitflow.domain.task.entity.ActivityType;
import io.streak.habitflow.domain.task.entity.TargetType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogRequest {
    private Long taskId;
    private ActivityType activityType;
    private TargetType targetType;
    private Long targetId;
}
