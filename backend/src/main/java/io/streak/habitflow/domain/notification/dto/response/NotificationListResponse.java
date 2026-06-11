package io.streak.habitflow.domain.notification.dto.response;

import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {
    private Long id;
    private Long receiverId;
    private Long actorId;
    private String actorName;
    private Long targetId;
    private NotificationType notificationType;
    private ActivityType activityType;
    private String customMessage;
    private boolean isConfirmed;
    private LocalDateTime createdAt;

    public static NotificationListResponse from(Notification notification) {
        return NotificationListResponse
                .builder()
                .id(notification.getId())
                .receiverId(notification.getReceiver().getId())
                .actorId(notification.getActor().getId())
                .actorName(notification.getActor().getName())
                .targetId(notification.getTargetId())
                .notificationType(notification.getNotificationType())
                .activityType(notification.getActivityType())
                .isConfirmed(notification.isConfirmed())
                .createdAt(notification.getCreatedAt())
                .customMessage(notification.getCustomMessage())
                .build();
    }
}
