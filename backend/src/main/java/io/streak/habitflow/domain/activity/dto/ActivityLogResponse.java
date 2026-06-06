package io.streak.habitflow.domain.activity.dto;

import io.streak.habitflow.domain.activity.entity.ActivityLog;
import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long id;
    private ActivityType activityType;

    public static ActivityLogResponse from(ActivityLog activityLog){
        return ActivityLogResponse.builder()
                .activityType(activityLog.getActivityType())
                .build();
    }

}
