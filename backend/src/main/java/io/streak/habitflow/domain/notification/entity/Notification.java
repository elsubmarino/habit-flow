package io.streak.habitflow.domain.notification.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.global.common.entity.BaseCreatedTimeEntity;
import io.streak.habitflow.global.common.type.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="notifications",indexes = {@Index(name="idx_notification_receiver_member_id",columnList = "receiver_member_id")})
public class Notification extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="notification_id")
    private Long id;

    @Builder.Default
    @Column(nullable = false,unique = true,updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="receiver_member_id", nullable = false)
    private Member receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="actor_member_id",nullable = false)
    private Member actor;

    @Column(name="target_id",nullable = false)
    private Long targetId;

    @Column(name="target_public_id",nullable = false)
    private Long targetPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name="notification_type",nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    private boolean isConfirmed;

    private String customMessage;

    public void updateConfirmationStatus(boolean confirmed){
        this.isConfirmed=confirmed;
    }
}
