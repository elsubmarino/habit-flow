package io.streak.habitflow.domain.task.entity;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.streak.habitflow.global.common.entity.BaseTimeEntity;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="tasks",indexes = {
        @Index(name="idx_tasks_cursor_paging_order",columnList = "project_id,parent_id,completed,due_date ASC,task_priority_type ASC,sort_order ASC,task_id DESC"),
        @Index(name="idx_tasks_today_count",columnList = "project_id,completed,parent_id,due_date"),
        @Index(name="idx_tasks_inbox_count",columnList = "member_id,completed,parent_id,project_id"),
        @Index(name="idx_tasks_parent_completed",columnList = "parent_id,completed")
})
public class Task extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="task_id")
    private Long id;

    @Builder.Default
    @Column(nullable = false,unique = true,updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false, length=100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskPriorityType taskPriorityType;

    @NotNull
    private Long sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id", nullable = false)
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

    private boolean completed;
    private LocalDateTime dueDate;

    private boolean recurring;

    private String recurrenceRule; //DAILY, WEEKLY, MONTHLY
    private Integer recurrenceInterval;  //1(매일,매주) 2(이틀마다, 격주 등)
    private String recurrenceDays; //MON, WED, FRI (WEEKLY 일 때 사용)
    private Integer recurrenceDayOfMonth; //11 (MONTHLY 일 때 사용. NULL 허용을 위해 Integer

    private boolean timeSpecified; //시간을 기록했느냐

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

    public void updateSortOrder(Long sortOrder){
        this.sortOrder = sortOrder;
    }

    public void updateCompleted(boolean completed){
        this.completed = completed;
    }

    public void updateDueDate(LocalDateTime targetDate){this.dueDate =targetDate;}

    public boolean updateSchedule(LocalDateTime newDueDate,  boolean newRecurring,
                                  String newRecurrenceRule, Integer newRecurrenceInterval,
                                  String newRecurrenceDays, Integer newRecurrenceDayOfMonth,
                                  boolean newTimeSpecified){
        boolean isChanged = false;
        if(!Objects.equals(this.dueDate,newDueDate)){
            this.dueDate = newDueDate;
            isChanged = true;
        }
        if(this.recurring != newRecurring){
            this.recurring = newRecurring;
            isChanged = true;
        }
        if (!Objects.equals(this.recurrenceRule, newRecurrenceRule)) {
            this.recurrenceRule = newRecurrenceRule;
            isChanged = true;
        }
        if (!Objects.equals(this.recurrenceInterval, newRecurrenceInterval)) {
            this.recurrenceInterval = newRecurrenceInterval;
            isChanged = true;
        }
        if (!Objects.equals(this.recurrenceDays, newRecurrenceDays)) {
            this.recurrenceDays = newRecurrenceDays;
            isChanged = true;
        }
        if (!Objects.equals(this.recurrenceDayOfMonth, newRecurrenceDayOfMonth)) {
            this.recurrenceDayOfMonth = newRecurrenceDayOfMonth;
            isChanged = true;
        }
        if (this.timeSpecified != newTimeSpecified) {
            this.timeSpecified = newTimeSpecified;
            isChanged = true;
        }

        validateRecurrence();
        return isChanged;
    }

    public void validateRecurrence(){
        if(recurring && (recurrenceRule == null || recurrenceRule.isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

}
