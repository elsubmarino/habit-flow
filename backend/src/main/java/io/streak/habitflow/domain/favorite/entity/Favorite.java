package io.streak.habitflow.domain.favorite.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.global.common.entity.BaseCreatedTimeEntity;
import io.streak.habitflow.global.common.type.TargetType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name="favorites",indexes = {@Index(name="idx_favorites_normal",columnList = "member_id,target_type,target_id")})
public class Favorite extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    private Long targetId;
}
