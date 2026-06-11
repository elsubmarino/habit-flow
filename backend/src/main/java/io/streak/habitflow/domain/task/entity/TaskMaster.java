package io.streak.habitflow.domain.task.entity;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.streak.habitflow.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="tasks")
public class TaskMaster extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="task_master_id")
    private Long id;

    @Column(nullable = false, length=100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskPriorityType taskPriorityType;

    private long sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_id")
    private TaskMaster parent;

    @OneToMany(mappedBy = "parent",cascade = CascadeType.ALL)
    @Builder.Default
    private List<TaskMaster> subTaskMasters = new ArrayList<>();

    @OneToMany(mappedBy = "taskMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "taskMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskLabel> taskLabels = new ArrayList<>();

    private boolean isRecurring;

    private String recurrenceRule; //DAILY, WEEKLY, MONTHLY
    private int recurrenceInterval;  //1(매일,매주) 2(이틀마다, 격주 등)
    private String recurrenceDays; //MON, WED, FRI (WEEKLY 일 때 사용)
    private Integer recurrenceDayOfMonth; //11 (MONTHLY 일 때 사용. NULL 허용을 위해 Integer

    @OneToMany(mappedBy = "taskMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskInstance> taskInstances = new ArrayList<>();


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

    public void updatePriorityType(TaskPriorityType taskPriorityType){
        this.taskPriorityType = taskPriorityType;
    }

    public void updateProject(Project project){
        this.project = project;
    }

    public void updateTaskLabels(List<TaskLabel> taskLabels){
        this.taskLabels = taskLabels;
    }

    public void changeProject(Project project){
        this.project = project;
    }

    public void updateRecurrenceInterval(int recurrenceInterval){
        this.recurrenceInterval = recurrenceInterval;
    }
    public void updateRecurrenceDays(String recurrenceDays){
        this.recurrenceDays = recurrenceDays;
    }
    public void updateRecurrenceDayOfMonth(Integer recurrenceDayOfMonth){
        this.recurrenceDayOfMonth = recurrenceDayOfMonth;
    }

}
