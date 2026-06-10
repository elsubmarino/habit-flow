package io.streak.habitflow.domain.activitylog.listener;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.domain.task.event.TaskCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {
    private final ActivityLogService activityLogService;

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent taskCompletedEvent) {
        ActivityLogRequest activityLogRequest = ActivityLogRequest.builder()
                .taskId(taskCompletedEvent.taskId())
                .activityType(taskCompletedEvent.activityType())
                .build();

        activityLogService.create(activityLogRequest,taskCompletedEvent.memberId());
    }
}
