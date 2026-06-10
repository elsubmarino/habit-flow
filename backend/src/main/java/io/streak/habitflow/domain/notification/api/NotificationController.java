package io.streak.habitflow.domain.notification.api;

import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationListResponse;
import io.streak.habitflow.domain.notification.service.NotificationService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationListResponse>> getNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal
                                                                       ) {
        return ResponseEntity.ok(notificationService.getNotifications(userPrincipal.getMemberId()));
    }

    @PutMapping("/{notificationId}/confirm")
    public ResponseEntity<Void> confirmNotification(@PathVariable Long notificationId,
                                                    @RequestBody NotificationRequest notificationRequest,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.confirmNotification(notificationId, notificationRequest, userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
