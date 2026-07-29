package io.streak.habitflow.global.security.auth;

import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("notificationAuth")
@RequiredArgsConstructor
public class NotificationAuth {
    private final NotificationRepository notificationRepository;
    public boolean canAccess(UUID publicNotificationId){
        Notification notification = notificationRepository.getOrThrowByPublicId(publicNotificationId);
        return notification.getReceiver().getPublicId().equals(publicNotificationId);
    }
}
