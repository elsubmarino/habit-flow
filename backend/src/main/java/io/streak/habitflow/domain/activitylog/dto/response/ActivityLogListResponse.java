package io.streak.habitflow.domain.activitylog.dto.response;

import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogListResponse {
    private Long id;
    private ActivityType activityType;
    private String userName;
    private String projectName;
    private LocalDateTime createdAt;
    private String customMessage;

    public static ActivityLogListResponse from(ActivityLog activityLog){
        return ActivityLogListResponse.builder()
                .id(activityLog.getId())
                .activityType(activityLog.getActivityType())
                .userName(activityLog.getMember() != null ? activityLog.getMember().getName() : "사용자")
                .createdAt(activityLog.getCreatedAt())
                .customMessage(activityLog.getCustomMessage())
                .build();
    }

}
