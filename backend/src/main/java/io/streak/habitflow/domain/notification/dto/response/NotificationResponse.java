package io.streak.habitflow.domain.notification.dto.response;

import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.Builder;

import java.time.LocalDateTime;

public final class NotificationResponse {
    @Builder
    public record List(
            Long id,
            Long receiverId,
            Long actorId,
            String actorName,
            Long targetId,
            NotificationType notificationType,
            ActivityType activityType,
            String customMessage,
            boolean isConfirmed,
            LocalDateTime createdAt
    ){
        public static List from(Notification notification) {
            return List
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
}
