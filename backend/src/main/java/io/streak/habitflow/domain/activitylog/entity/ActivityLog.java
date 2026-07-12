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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="actor_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member actor;

    private Long targetId;

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
