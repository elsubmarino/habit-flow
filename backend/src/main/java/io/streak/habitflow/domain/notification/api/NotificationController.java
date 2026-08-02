package io.streak.habitflow.domain.notification.api;

import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationResponse;
import io.streak.habitflow.domain.notification.service.NotificationService;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.infra.sse.SseEmitters;
import io.streak.habitflow.global.web.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final SseEmitters sseEmitters;

    @GetMapping
    @Operation(summary = "알림 다건 조회")
    public ResponseEntity<List<NotificationResponse.Summary>> getNotifications(@LoginMemberId Long loginMemberId
                                                                       ) {
        return ResponseEntity.ok(notificationService.getNotifications(loginMemberId));
    }

    @PutMapping("/{publicNotificationId}/confirm")
    @Operation(summary = "알림 확인")
    public ResponseEntity<NotificationResponse.Summary> confirmNotification(@PathVariable UUID publicNotificationId,
                                                                            @RequestBody NotificationRequest.ConfirmRead request,
                                                                            @LoginMemberId Long loginMemberId) {
        return ResponseEntity.ok(notificationService.updateNotificationConfirmation(publicNotificationId, request, loginMemberId));
    }

    @GetMapping(value="/subscribe",produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "알림 구독(SSE)")
    public ResponseEntity<SseEmitter> subscribe(@LoginMemberId Long loginMemberId){
        SseEmitter emitter = new SseEmitter(1000L * 60 * 30); //30분
        try{
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        }catch(IOException e){
            throw new BusinessException(ErrorCode.SSE_CONNECTION_FAILED);
        }
        sseEmitters.add(loginMemberId, emitter);
        return ResponseEntity.ok(emitter);
    }
}
