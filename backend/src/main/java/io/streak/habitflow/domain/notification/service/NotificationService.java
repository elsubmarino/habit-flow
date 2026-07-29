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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final HashidsProvider hashidsProvider;

    public List<NotificationResponse.Summary> getNotifications(Long memberId) {
        Member receiver = memberRepository.getReferenceById(memberId);
        List<Notification> notifications = notificationRepository.findByReceiver(receiver);
        return notifications.stream()
                .map(response->{
                    return NotificationResponse.Summary.of(response,response.getPublicId().toString(),receiver.getPublicId().toString(),
                            response.getActor().getPublicId().toString(),response.getTargetPublicId().toString());
                })
                .toList();
    }

    @Transactional
    @CheckOwnership(type="NOTIFICATION")
    @SuppressWarnings("unused")
    public NotificationResponse.Summary confirmNotification(UUID publicId, NotificationRequest.ConfirmRead request, Long loginMemberId) {
        Notification notification = notificationRepository.getOrThrowByPublicId(publicId);
        notification.updateConfirmed(request.confirmed());
        return NotificationResponse.Summary.of(notification,notification.getPublicId().toString(),notification.getReceiver().getPublicId().toString(),
                notification.getActor().getPublicId().toString(),notification.getTargetPublicId().toString());
    }
}