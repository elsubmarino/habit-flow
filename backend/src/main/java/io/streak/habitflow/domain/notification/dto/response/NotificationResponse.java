package io.streak.habitflow.domain.notification.dto.response;

import io.streak.habitflow.domain.notification.entity.Notification;
import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public final class NotificationResponse {
    @Builder
    public record Summary(
            @Schema(description = "알림 ID",example="6q9WeDv5")
            String id,

            @Schema(description = "받는 사람의 ID",example="6q9WeDv5")
            String receiverId,

            @Schema(description = "알림을 발생시킨 사람의 ID",example="6q9WeDv5")
            String actorId,

            @Schema(description = "알림을 발생시킨 사람의 이름",example="홍길동")
            String actorName,

            @Schema(description = "알림을 받는 자의 id",example="6q9WeDv5")
            String targetId,

            @Schema(description = "알림의 대상 타입",examples={"TASK","PROJECT"})
            NotificationType notificationType,

            @Schema(description = """
                        액티비티 타입(
                        ADDED,
                        COMPLETED,
                        UPDATED,
                        DELETED,
                        MOVED,
                        INVITED,
                        UNCOMPLETED,
                        JOINED)
                    """,examples={"ADDED","COMPLETED"})
            ActivityType activityType,

            @Schema(description = "알림 내용")
            String customMessage,

            @Schema(description = "알림 확인 여부")
            boolean isConfirmed,

            @Schema(description = "알림 생성 일자")
            LocalDateTime createdAt
    ){
        public static Summary of(Notification notification,String encodedId,
                                 String encodedReceiverId,
                                 String encodedActorId,
                                 String encodedTargetId) {
            return Summary
                    .builder()
                    .id(encodedId)
                    .receiverId(encodedReceiverId)
                    .actorId(encodedActorId)
                    .actorName(notification.getActor().getName())
                    .targetId(encodedTargetId)
                    .notificationType(notification.getNotificationType())
                    .activityType(notification.getActivityType())
                    .isConfirmed(notification.isConfirmed())
                    .createdAt(notification.getCreatedAt())
                    .customMessage(notification.getCustomMessage())
                    .build();
        }
    }
}
