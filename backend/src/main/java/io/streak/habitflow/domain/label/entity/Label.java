package io.streak.habitflow.domain.label.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="labels",indexes = {
        @Index(name="idx_labels_member_paging",columnList = "member_id,label_id DESC")
})
public class Label  extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="label_id")
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true,updatable = false)
    private UUID publicId = UUID.randomUUID();

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

    public void updateSortOrder(Long sortOrder){
        this.sortOrder = sortOrder;
    }
}
