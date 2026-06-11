package io.streak.habitflow.domain.notification.service;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationListResponse;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.infra.sse.SseEmitters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SseEmitters sseEmitters;
    private final MemberRepository memberRepository;

    @Transactional
    public void createNotification(NotificationRequest notificationRequest, Long receiverId, Long actorId) {
        Member receiver = memberRepository.getReferenceById(receiverId);
        Member actor = memberRepository.getReferenceById(actorId);

        Notification notification = Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .targetId(notificationRequest.getTargetId())
                .notificationType(notificationRequest.getNotificationType())
                .activityType(notificationRequest.getActivityType())
                .isConfirmed(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        NotificationListResponse notificationListResponse = NotificationListResponse.from(savedNotification);
        sseEmitters.sendToMember(receiverId,notificationListResponse);
    }

    public List<NotificationListResponse> getNotifications(Long memberId) {
        List<Notification> notifications = notificationRepository.findByMemberId(memberId);
        return notifications.stream()
                .map(NotificationListResponse::from)
                .toList();
    }

    @Transactional
    @CheckOwnership(type="NOTIFICATION")
    @SuppressWarnings("unused")
    public void confirmNotification(Long notificationId, NotificationRequest notificationRequest, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow();
        notification.confirmNotification(notificationRequest.isConfirmed());
    }
}