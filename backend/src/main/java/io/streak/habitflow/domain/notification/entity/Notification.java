package io.streak.habitflow.domain.notification.entity;

import io.streak.habitflow.common.jpa.BaseCreatedTimeEntity;
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
public class Notification extends BaseCreatedTimeEntity {
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

    private boolean isConfirmed;

}
