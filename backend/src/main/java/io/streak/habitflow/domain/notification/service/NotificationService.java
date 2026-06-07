package io.streak.habitflow.domain.notification.service;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationListResponse;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @SuppressWarnings("unused")
    public void createNotification(NotificationRequest notificationRequest){
        Notification notification = Notification.builder()
                .activityType(notificationRequest.getActivityType())
                .build();
        notificationRepository.save(notification);
    }

    public List<NotificationListResponse> getNotifications(UserDetails userDetails){
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        List<Notification> notifications = notificationRepository.findByMemberId(member.getId());
        return notifications.stream()
                .map(NotificationListResponse::from)
                .toList();
    }

    @Transactional
    public void confirmNotification(Long id, NotificationRequest notificationRequest, UserDetails userDetails){
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("알림이 존재하지 않습니다."));
        if(!notification.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        notification.confirmNotification(notificationRequest.isConfirmed());
    }
}