package io.streak.habitflow.domain.notification.entity;

import io.streak.habitflow.global.common.BaseCreatedTimeEntity;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.member.entity.Member;
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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    private boolean isConfirmed;

    public void confirmNotification(boolean isConfirmed){
        this.isConfirmed=isConfirmed;
    }
}
