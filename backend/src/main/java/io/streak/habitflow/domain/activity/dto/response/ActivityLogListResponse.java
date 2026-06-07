package io.streak.habitflow.domain.activity.dto.response;

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
public class ActivityLogListResponse {
    private Long id;
    private ActivityType activityType;

    public static ActivityLogListResponse from(ActivityLog activityLog){
        return ActivityLogListResponse.builder()
                .activityType(activityLog.getActivityType())
                .id(activityLog.getId())
                .build();
    }

}
