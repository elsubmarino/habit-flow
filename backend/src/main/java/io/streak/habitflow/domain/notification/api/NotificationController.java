package io.streak.habitflow.domain.notification.api;

import io.streak.habitflow.domain.notification.dto.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.NotificationResponse;
import io.streak.habitflow.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * 알림 다건 조회
     * @param userDetails 인증된 사용자 정보
     * @return 알림 응답 정보 DTO
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal UserDetails userDetails
                                                                       ) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails));
    }

    /**
     * 알림 업데이트
     * @param id 알림 ID
     * @param notificationRequest 알림 요청 정보 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 알림 응답 정보 DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationResponse> updateNotification(@PathVariable Long id, @RequestBody NotificationRequest notificationRequest
            , @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.updateNotification(id,notificationRequest,userDetails));
    }
}
