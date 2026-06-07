package io.streak.habitflow.domain.notification.api;

import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationListResponse;
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

    @GetMapping
    public ResponseEntity<List<NotificationListResponse>> getNotifications(@AuthenticationPrincipal UserDetails userDetails
                                                                       ) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmNotification(@PathVariable Long id,
                                                                        @RequestBody NotificationRequest notificationRequest,
                                                                        @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.confirmNotification(id, notificationRequest, userDetails);
        return ResponseEntity.noContent().build();
    }
}
