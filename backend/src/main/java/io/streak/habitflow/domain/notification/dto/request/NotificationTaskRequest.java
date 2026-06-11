package io.streak.habitflow.domain.notification.dto.request;

import io.streak.habitflow.domain.task.type.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTaskRequest {
    private Long id;
    private Long taskId;
    private ActivityType activityType;
    private boolean isConfirmed;
}
