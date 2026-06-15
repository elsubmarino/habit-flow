package io.streak.habitflow.domain.notification.repository;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByReceiver(Member receiver);

    default Notification getOrThrow(Long notificationId) {
        return findById(notificationId).orElseThrow(()->new IllegalArgumentException("존재하지 않는 알림이니다."));
    }
}
