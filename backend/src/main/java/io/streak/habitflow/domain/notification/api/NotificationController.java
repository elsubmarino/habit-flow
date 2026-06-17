package io.streak.habitflow.domain.notification.api;

import io.streak.habitflow.domain.notification.dto.request.NotificationRequest;
import io.streak.habitflow.domain.notification.dto.response.NotificationResponse;
import io.streak.habitflow.domain.notification.service.NotificationService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.streak.habitflow.global.infra.sse.SseEmitters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final SseEmitters sseEmitters;

    @GetMapping
    @Operation(summary = "알림 다건 조회")
    public ResponseEntity<List<NotificationResponse.List>> getNotifications(@LoginMemberId Long loginMemberId
                                                                       ) {
        return ResponseEntity.ok(notificationService.getNotifications(loginMemberId));
    }

    @PutMapping("/{notificationId}/confirm")
    @Operation(summary = "알림 확인")
    public ResponseEntity<Void> confirmNotification(@PathVariable Long notificationId,
                                                    @RequestBody NotificationRequest.Create request,
                                                    @LoginMemberId Long loginMemberId) {
        notificationService.confirmNotification(notificationId, request, loginMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value="/subscribe",produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "알림 구독(SSE)")
    public ResponseEntity<SseEmitter> subscribe(@LoginMemberId Long loginMemberId){
        SseEmitter emitter = new SseEmitter(50*1000L);
        try{
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        }catch(IOException e){
            throw new RuntimeException("SSE 최초 연결 더미 데이터 전송 실패",e);
        }
        sseEmitters.add(loginMemberId, emitter);
        return ResponseEntity.ok(emitter);
    }
}
