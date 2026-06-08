package io.streak.habitflow.domain.task.entity;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.task.type.PriorityType;
import io.streak.habitflow.global.common.BaseTimeEntity;
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
@Table(name="tasks")
public class Task extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    private boolean completed;

    @Enumerated(EnumType.STRING)
    private PriorityType priorityType;

    private LocalDateTime dueDate;
    private long sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id")
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
    private List<TaskLabel> taskLabels = new ArrayList<>();

    public void addTaskLabel(TaskLabel taskLabel){
        this.taskLabels.add(taskLabel);
        taskLabel.assignTask(this);
    }

    public void addComment(Comment comment){
        this.comments.add(comment);
        comment.assignTask(this);
    }

    public void updateName(String name){
        this.name = name;
    }

    public void updateDescription(String description){
        this.description = description;
    }

    public void updatePriorityType(PriorityType priorityType){
        this.priorityType = priorityType;
    }

    public void updateDueDate(LocalDateTime dueDate){
        this.dueDate = dueDate;
    }

    public void changeProject(Project project){
        this.project = project;
    }

    public void updateCompleted(boolean completed){
        this.completed = completed;
    }
}
