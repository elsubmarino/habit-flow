package io.streak.habitflow.domain.notification.service;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationResponse;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    public List<NotificationResponse.Summary> getNotifications(Long memberId) {
        Member receiver = memberRepository.getReferenceById(memberId);
        List<Notification> notifications = notificationRepository.findByReceiver(receiver);
        return notifications.stream()
                .map(NotificationResponse.Summary::from)
                .toList();
    }

    @Transactional
    @CheckOwnership(type="NOTIFICATION")
    @SuppressWarnings("unused")
    public NotificationResponse.Summary confirmNotification(Long notificationId, NotificationRequest.Create request, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow();
        notification.confirmNotification(request.isConfirmed());
        return NotificationResponse.Summary.from(notification);
    }
}