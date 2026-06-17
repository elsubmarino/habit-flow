package io.streak.habitflow.domain.task.entity;

import io.streak.habitflow.domain.label.entity.Label;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name="task_labels")
public class TaskLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="task_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_master_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Label label;

    public void assignTask(Task task){
        this.task = task;
    }
}
