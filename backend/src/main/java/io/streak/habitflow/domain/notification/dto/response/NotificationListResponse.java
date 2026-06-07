package io.streak.habitflow.domain.notification.dto.response;

import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {
    private Long taskId;
    private ActivityType activityType;
    private boolean isConfirmed;

    public static NotificationListResponse from(Notification notification) {
        return NotificationListResponse
                        .builder()
                        .activityType(notification.getActivityType())
                        .isConfirmed(notification.isConfirmed())
                        .taskId(notification.getTask().getId())
                        .build();
    }
}
