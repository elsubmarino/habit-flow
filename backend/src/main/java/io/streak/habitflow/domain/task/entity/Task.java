package io.streak.habitflow.domain.task.entity;

import io.streak.habitflow.global.common.BaseTimeEntity;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class Task extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private boolean isCompleted;

    @Enumerated(EnumType.STRING)
    private PriorityType priorityType;

    private LocalDateTime dueDate;
    private long sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_id")
    private Task parent;

    @OneToMany(mappedBy = "parent",cascade = CascadeType.ALL)
    @Builder.Default
    private List<Task> subTasks = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Label>  labels = new ArrayList<>();
}
