package io.streak.habitflow.domain.notification.dto.response;

import io.streak.habitflow.domain.notification.type.NotificationType;

import java.time.LocalDateTime;

public record ProjectNotificationResponse (
        NotificationType notificationType,
        Long projectId,
        Long projectName,
        String inviteName,
        String message,
        LocalDateTime createdAt
){}
