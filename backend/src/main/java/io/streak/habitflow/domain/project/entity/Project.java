package io.streak.habitflow.domain.project.entity;

import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import io.streak.habitflow.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="projects")
public class Project  extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="project_id")
    private Long id;

    private String name;
    private String color;

    @Enumerated(EnumType.STRING)
    private AccessType accessType;

    @Enumerated(EnumType.STRING)
    private LayoutType layoutType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Project parent;

    public void updateProject(String name,
                              String color,
                              AccessType accessType,
                              LayoutType layoutType,
                              Project parent) {
        this.name=name;
        this.color=color;
        this.accessType = accessType;
        this.layoutType = layoutType;
        this.parent = parent;
    }
}
