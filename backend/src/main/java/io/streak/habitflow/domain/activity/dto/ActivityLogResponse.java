package io.streak.habitflow.domain.activity.dto;

import io.streak.habitflow.domain.activity.entity.ActivityLog;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityLogResponse {
    private Long id;
    private Long taskId;

    public static ActivityLogResponse from(ActivityLog activityLog){
        ActivityLogResponseBuilder builder =  ActivityLogResponse.builder()
                .id(activityLog.getId());

        if(activityLog.getTask() != null){
            builder.taskId(activityLog.getTask().getId());
        }


        return builder.build();
    }

}
