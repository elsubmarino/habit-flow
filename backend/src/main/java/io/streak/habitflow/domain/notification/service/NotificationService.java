package io.streak.habitflow.domain.notification.service;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationResponse;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.util.HashidsProvider;
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
    private final HashidsProvider hashidsProvider;

    public List<NotificationResponse.Summary> getNotifications(Long memberId) {
        Member receiver = memberRepository.getReferenceById(memberId);
        String encodedReceiverId = hashidsProvider.encode(receiver.getId());
        List<Notification> notifications = notificationRepository.findByReceiver(receiver);
        return notifications.stream()
                .map(response->{
                    String encodedId = hashidsProvider.encode(response.getId());
                    String encodedActorId = hashidsProvider.encode(response.getActor().getId());
                    String encodedTargetId = hashidsProvider.encode(response.getTargetId());
                    return NotificationResponse.Summary.of(response,encodedId,encodedReceiverId,
                            encodedActorId,encodedTargetId);
                })
                .toList();
    }

    @Transactional
    @CheckOwnership(type="NOTIFICATION")
    @SuppressWarnings("unused")
    public NotificationResponse.Summary confirmNotification(Long notificationId, NotificationRequest.ConfirmRead request, Long loginMemberId) {
        Notification notification = notificationRepository.getOrThrow(notificationId);
        notification.updateConfirmed(request.confirmed());
        String encodedId = hashidsProvider.encode(notification.getId());
        String encodedReceiverId = hashidsProvider.encode(notification.getReceiver().getId());
        String encodedActorId = hashidsProvider.encode(notification.getActor().getId());
        String encodedTargetId = hashidsProvider.encode(notification.getTargetId());
        return NotificationResponse.Summary.of(notification,encodedId,encodedReceiverId,
                encodedActorId,encodedTargetId);
    }
}