package io.streak.habitflow.domain.notification.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.notification.type.NotificationType;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.common.entity.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="notifications")
public class Notification extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="receiver_member_id", nullable = false,foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="actor_member_id",nullable = false,foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member actor;

    @Column(name="target_id",nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name="notification_type",nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    private boolean isConfirmed;

    private String customMessage;

    public void confirmNotification(boolean isConfirmed){
        this.isConfirmed=isConfirmed;
    }
}
