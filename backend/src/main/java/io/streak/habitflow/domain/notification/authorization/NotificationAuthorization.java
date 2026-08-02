package io.streak.habitflow.domain.notification.authorization;

import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("notificationAuthorization")
@RequiredArgsConstructor
public class NotificationAuthorization {
    private final NotificationRepository notificationRepository;
    public boolean canAccess(UUID publicNotificationId){
        Notification notification = notificationRepository.getOrThrowByPublicId(publicNotificationId);
        return notification.getReceiver().getPublicId().equals(publicNotificationId);
    }
}
