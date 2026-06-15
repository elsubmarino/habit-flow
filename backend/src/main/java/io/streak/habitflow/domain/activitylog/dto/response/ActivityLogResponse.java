package io.streak.habitflow.domain.activitylog.dto.response;

import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.Builder;

import java.time.LocalDateTime;

public final class ActivityLogResponse {
    @Builder
    public record List(
            Long id,
            ActivityType activityType,
            String userName,
            String projectName,
            LocalDateTime createdAt,
            String customMessage
    ){
        public static List from(ActivityLog activityLog){
            return List.builder()
                    .id(activityLog.getId())
                    .activityType(activityLog.getActivityType())
                    .userName(activityLog.getMember() != null ? activityLog.getMember().getName() : "사용자")
                    .createdAt(activityLog.getCreatedAt())
                    .customMessage(activityLog.getCustomMessage())
                    .build();
        }
    }
}
