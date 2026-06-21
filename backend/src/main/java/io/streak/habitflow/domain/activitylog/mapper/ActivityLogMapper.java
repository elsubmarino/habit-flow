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
                    .id(hashidsProvider.encode(activityLog.getActor().getId()))
                    .name(activityLog.getActor().getName())
                    .build();
        }

        ActivityLogResponse.TargetInfo targetInfo = ActivityLogResponse.TargetInfo.builder()
                .type(activityLog.getTargetType())
                .id(hashidsProvider.encode(activityLog.getTargetId()))
                .name(activityLog.getTargetName())
                .build();

        return ActivityLogResponse.Summary.builder()
                .id(hashidsProvider.encode(activityLog.getId()))
                .activityType(activityLog.getActivityType())
                .actor(actorInfo)
                .target(targetInfo)
                .createdAt(activityLog.getCreatedAt())
                .changes(activityLog.getChanges())
                .build();
    }
}
