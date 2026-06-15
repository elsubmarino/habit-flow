package io.streak.habitflow.domain.activitylog.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import io.streak.habitflow.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_project_created_at", columnList = "project_id,created_at"),
        @Index(name = "idx_member_created_at", columnList = "member_id,created_at")
})
public class ActivityLog extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="activity_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TargetType targetType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private ActivityType activityType;

    private String customMessage;

}
