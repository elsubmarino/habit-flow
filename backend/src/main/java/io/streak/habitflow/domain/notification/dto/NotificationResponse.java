package io.streak.habitflow.domain.notification.dto;

import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.task.entity.ActivityType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long taskId;
    private ActivityType activityType;
    private boolean isConfirmed;

    public static NotificationResponse from(Notification notification) {
        return
                NotificationResponse
                        .builder()
                        .activityType(notification.getActivityType())
                        .isConfirmed(notification.isConfirmed())
                        .taskId(notification.getTask().getId())
                        .build();
    }
}
