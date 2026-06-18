package io.streak.habitflow.domain.activitylog.mapper;

import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogResponse;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogMapper {
    public ActivityLogResponse.Summary toSummary(ActivityLog activityLog){
        ActivityLogResponse.ActorInfo actorInfo = null;
        if(activityLog.getActor() != null){
            actorInfo = ActivityLogResponse.ActorInfo.builder()
                    .id(activityLog.getActor().getId())
                    .name(activityLog.getActor().getName())
                    .build();
        }

        ActivityLogResponse.TargetInfo targetInfo = ActivityLogResponse.TargetInfo.builder()
                .type(activityLog.getTargetType())
                .id(activityLog.getTargetId())
                .name(activityLog.getTargetName())
                .build();

        return ActivityLogResponse.Summary.builder()
                .id(activityLog.getId())
                .activityType(activityLog.getActivityType())
                .actor(actorInfo)
                .target(targetInfo)
                .createdAt(activityLog.getCreatedAt())
                .changes(activityLog.getChanges())
                .build();
    }
}
