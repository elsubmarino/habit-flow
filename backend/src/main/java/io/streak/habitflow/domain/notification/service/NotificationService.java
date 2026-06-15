package io.streak.habitflow.domain.notification.service;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationResponse;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.infra.sse.SseEmitters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SseEmitters sseEmitters;
    private final MemberRepository memberRepository;

    @Transactional
    public void createNotification(NotificationRequest.Create request, Long receiverId, Long actorId) {
        Member receiver = memberRepository.getReferenceById(receiverId);
        Member actor = memberRepository.getReferenceById(actorId);

        Notification notification = Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .targetId(request.targetId())
                .notificationType(request.notificationType())
                .activityType(request.activityType())
                .customMessage(request.customMessage())
                .isConfirmed(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        NotificationResponse.List notificationListResponse = NotificationResponse.List.from(savedNotification);
        sseEmitters.sendToMember(receiverId,notificationListResponse);
    }

    public List<NotificationResponse.List> getNotifications(Long memberId) {
        Member receiver = memberRepository.getReferenceById(memberId);
        List<Notification> notifications = notificationRepository.findByReceiver(receiver);
        return notifications.stream()
                .map(NotificationResponse.List::from)
                .toList();
    }

    @Transactional
    @CheckOwnership(type="NOTIFICATION")
    @SuppressWarnings("unused")
    public void confirmNotification(Long notificationId, NotificationRequest.Create request, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow();
        notification.confirmNotification(request.isConfirmed());
    }
}