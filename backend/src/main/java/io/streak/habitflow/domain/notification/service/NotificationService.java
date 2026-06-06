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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    /**
     * 알림 생성
     * @param notificationRequest 알림 요청 DTO 정보
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
     * @param userDetails 인증된 사용자 정보
     * @return 조회된 다건의 알림 DTO 정보
     */
    public List<NotificationResponse> getNotifications(UserDetails userDetails){
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        List<Notification> notifications = notificationRepository.findByMemberId(member.getId());
        return notifications.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * 알림 업데이트
     * @param id 알림 ID
     * @param notificationRequest 요청된 알림 DTO 정보
     * @param userDetails 인증된 사용자 정보
     * @return 업데이트 된 알림 DTO 응답 정보
     */
    @Transactional
    public NotificationResponse updateNotification(Long id,NotificationRequest notificationRequest,UserDetails userDetails){
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("알림이 존재하지 않습니다."));
        if(!notification.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        notification.updateNotification(notificationRequest.isConfirmed());
        return NotificationResponse.from(notification);
    }
}