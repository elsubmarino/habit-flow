package io.streak.habitflow.domain.notification.service;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.notification.dto.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.NotificationResponse;
import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    /**
     * 알림 생성
     * @param notificationRequest
     */
    @Transactional
    public void createNotification(NotificationRequest notificationRequest){
        Notification notification = Notification.builder()
                .activityType(notificationRequest.getActivityType())
                .build();
        notificationRepository.save(notification);
    }

    /**
     * 알림 다건 조회
     * @param notificationRequest
     * @param userDetails
     * @return
     */
    public List<NotificationResponse> getNotifications(NotificationRequest notificationRequest,UserDetails userDetails){
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        List<Notification> notifications = notificationRepository.findByUserId(member.getId())
                .orElseThrow(()->new IllegalArgumentException("알림이 없습니다."));
        return notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 업데이트
     * @param id
     * @param notificationRequest
     * @param userDetails
     * @return
     */
    @Transactional
    public NotificationResponse updateNotification(Long id,NotificationRequest notificationRequest,UserDetails userDetails){
        Notification notification = Notification.builder()
                        .id(id)
                        .isConfirmed(notificationRequest.isConfirmed())
                        .build();
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}