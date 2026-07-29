package io.streak.habitflow.domain.activitylog.entity;

import io.streak.habitflow.domain.activitylog.vo.ChangeSet;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.global.common.entity.BaseCreatedTimeEntity;
import io.streak.habitflow.global.common.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_log_member_paging", columnList = "member_id, activity_log_id DESC")
})
public class ActivityLog extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="activity_log_id")
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true,updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="actor_id")
    private Member actor;

    private Long targetId;

    @Builder.Default
    @Column(nullable = false, unique = true,updatable = false)
    private UUID targetPublicId = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TargetType targetType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private ActivityType activityType;

    private String targetName;

    @Convert(converter = ChangeSetListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<ChangeSet> changes = new ArrayList<>();

}
