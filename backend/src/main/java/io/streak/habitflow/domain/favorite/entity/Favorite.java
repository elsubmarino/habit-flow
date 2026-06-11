package io.streak.habitflow.domain.favorite.entity;

import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name="favorites")
public class Favorite extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    private Long targetId;
}
