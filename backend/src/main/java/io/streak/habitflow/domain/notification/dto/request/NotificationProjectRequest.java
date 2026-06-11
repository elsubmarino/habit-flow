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
public class NotificationProjectRequest {
    private Long projectId;
    private Long memberId;
    private ActivityType activityType;
}
