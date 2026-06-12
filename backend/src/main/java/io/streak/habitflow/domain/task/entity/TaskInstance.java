package io.streak.habitflow.domain.task.entity;

import io.streak.habitflow.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name="task_instances",indexes={
        @Index(name="idx_instance_due_date",columnList="due_date")
})
public class TaskInstance extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="task_instance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_master_id",nullable = false)
    private TaskMaster taskMaster;

    private LocalDate dueDate;
    private boolean isCompleted;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;

    public void updateCompleted(boolean isCompleted){
        this.isCompleted = isCompleted;
    }

    public void updateDueDate(LocalDate targetDate){this.dueDate =targetDate;}
}
