package io.streak.habitflow.domain.activitylog.mapper;

import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogResponse;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogMapper {
    private final HashidsProvider hashidsProvider;
    public ActivityLogResponse.Summary toSummary(ActivityLog activityLog){
        ActivityLogResponse.ActorInfo actorInfo = null;
        if(activityLog.getActor() != null){
            actorInfo = ActivityLogResponse.ActorInfo.builder()
                    .id(activityLog.getActor().getPublicId().toString())
                    .name(activityLog.getActor().getName())
                    .build();
        }

        ActivityLogResponse.TargetInfo targetInfo = ActivityLogResponse.TargetInfo.builder()
                .type(activityLog.getTargetType())
                .id(activityLog.getTargetPublicId().toString())
                .name(activityLog.getTargetName())
                .build();

        return ActivityLogResponse.Summary.builder()
                .id(activityLog.getPublicId().toString())
                .activityType(activityLog.getActivityType())
                .actor(actorInfo)
                .target(targetInfo)
                .createdAt(activityLog.getCreatedAt())
                .changes(activityLog.getChanges())
                .build();
    }
}
