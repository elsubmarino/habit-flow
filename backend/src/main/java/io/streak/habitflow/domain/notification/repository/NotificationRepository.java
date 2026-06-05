package io.streak.habitflow.domain.notification.repository;

import io.streak.habitflow.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    Optional<List<Notification>> findByUserId(Long userId);
}
