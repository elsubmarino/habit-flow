package io.streak.habitflow.domain.activity.dto;

import io.streak.habitflow.domain.activity.entity.ActivityLog;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long id;
    private ActivityType activityType;
    private TargetType targetType;
    private Long targetId;

    public static ActivityLogResponse from(ActivityLog activityLog){
        return ActivityLogResponse.builder()
                .activityType(activityLog.getActivityType())
                .build();
    }

}
