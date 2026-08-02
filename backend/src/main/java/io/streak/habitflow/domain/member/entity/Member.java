package io.streak.habitflow.domain.member.entity;

import io.streak.habitflow.domain.member.type.MemberRole;
import io.streak.habitflow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="members",
    uniqueConstraints = {
        @UniqueConstraint(name="uk_members_email",columnNames = "email"),
        @UniqueConstraint(name="uk_members_public_id",columnNames = "public_id")
    })
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="member_id")
    private Long id;

    @Builder.Default
    @Column(nullable = false,updatable = false)
    private UUID publicId = UUID.randomUUID();

    private String name;
    private String password;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole memberRole;

    public void updatePassword(String password){
        this.password=password;
    }
    public void updateName(String name){
        this.name=name;
    }

}
