package io.streak.habitflow.domain.notification.repository;

import io.streak.habitflow.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByMemberId(Long userId);
}
