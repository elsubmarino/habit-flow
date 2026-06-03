package io.streak.habitflow.domain.notification.dto;

import io.streak.habitflow.domain.task.entity.ActivityType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
    private Long taskId;
    private ActivityType activityType;
    private boolean isConfirmed;
}
