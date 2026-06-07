package io.streak.habitflow.domain.activity.dto.response;

import io.streak.habitflow.domain.activity.entity.ActivityLog;
import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    public static ActivityLogListResponse from(ActivityLog activityLog){
        return ActivityLogListResponse.builder()
                .id(activityLog.getId())
                .activityType(activityLog.getActivityType())
                .userName(activityLog.getMember() != null ? activityLog.getMember().getName() : "사용자")
                .projectName(activityLog.getProject() != null ? activityLog.getProject().getName() : "사용자")
                .createdAt(activityLog.getCreatedAt())
                .build();
    }

}
