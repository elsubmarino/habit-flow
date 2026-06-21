package io.streak.habitflow.domain.project.entity;

import io.streak.habitflow.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
@Builder
@Table(name="project_members",uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_member",columnNames = {"project_id","member_id"}),
},indexes = {
        @Index(name="idx_project_members_member",columnList = "member_id")
})
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="project_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="project_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;
}
