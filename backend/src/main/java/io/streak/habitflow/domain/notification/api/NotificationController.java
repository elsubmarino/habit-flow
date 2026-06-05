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
@RequestMapping("/api/notification")
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * 알림 다건 조회
     * @param userDetails
     * @param notificationRequest
     * @return
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal UserDetails userDetails,
                                                                       @RequestBody NotificationRequest notificationRequest
                                                                       ) {
        List<NotificationResponse> notificationResponses = notificationService.getNotifications(notificationRequest,userDetails);
        return ResponseEntity.ok(notificationResponses);
    }

    /**
     * 알림 업데이트
     * @param id
     * @param notificationRequest
     * @param userDetails
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationResponse> updateNotification(@PathVariable Long id, @RequestBody NotificationRequest notificationRequest
            , @AuthenticationPrincipal UserDetails userDetails) {
        NotificationResponse notificationResponse = notificationService.updateNotification(id,notificationRequest,userDetails);
        return ResponseEntity.ok(notificationResponse);
    }
}
