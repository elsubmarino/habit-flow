package io.streak.habitflow.domain.notification.repository;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByReceiver(Member receiver);
    Optional<Notification> findByPublicId(UUID publicId);


    default Notification getOrThrow(Long notificationId) {
        return findById(notificationId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }

    default Notification getOrThrowByPublicId(UUID publicId) {
        return findByPublicId(publicId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
}
