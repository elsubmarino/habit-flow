package io.streak.habitflow.domain.notification.dto.request;

import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.global.common.type.ActivityType;
import lombok.Builder;

public final class NotificationRequest {
    @Builder
    public record Create(
            Long targetId,
            NotificationType notificationType,
            ActivityType activityType,
            boolean isConfirmed,
            String customMessage
    ){}

    public record ConfirmRead(boolean confirmed) {}
}
