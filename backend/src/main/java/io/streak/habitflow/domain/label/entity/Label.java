package io.streak.habitflow.domain.label.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="labels")
public class Label  extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="label_id")
    private Long id;

    private String name;
    private String color;
    private long sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id")
    private Member member;

    public void updateLabel(String name, String color){
        this.name = name;
        this.color = color;
    }
}
