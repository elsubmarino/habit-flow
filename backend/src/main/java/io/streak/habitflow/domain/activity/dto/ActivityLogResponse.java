package io.streak.habitflow.domain.activity.dto;

import io.streak.habitflow.domain.activity.entity.ActivityLog;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long id;
    private Long taskId;

    public static ActivityLogResponse from(ActivityLog activityLog){
        ActivityLogResponseBuilder builder =  ActivityLogResponse.builder()
                .id(activityLog.getId());


        return builder.build();
    }

}
