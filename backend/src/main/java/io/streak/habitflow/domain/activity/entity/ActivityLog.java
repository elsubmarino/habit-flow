package io.streak.habitflow.domain.activity.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_paroject_created_at", columnList = "project_id,created_at"),
        @Index(name = "idx_member_created_at", columnList = "member_id,created_at")
})
public class ActivityLog extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

}
