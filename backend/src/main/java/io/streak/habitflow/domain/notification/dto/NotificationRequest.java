package io.streak.habitflow.domain.notification.dto;

import io.streak.habitflow.domain.task.entity.ActivityType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long id;
    private Long taskId;
    private ActivityType activityType;
    private boolean isConfirmed;
}
