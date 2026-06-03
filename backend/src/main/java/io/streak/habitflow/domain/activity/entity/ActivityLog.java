package io.streak.habitflow.domain.activity.entity;

import io.streak.habitflow.domain.task.entity.ActivityType;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(indexes = {
        @Index(name="idx_task_created_at",columnList = "task_id,created_at"),
        @Index(name="idx_user_created_at",columnList = "user_id,created_at")
})
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;
}
