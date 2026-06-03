package io.streak.habitflow.domain.notification.repository;

import io.streak.habitflow.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
}
