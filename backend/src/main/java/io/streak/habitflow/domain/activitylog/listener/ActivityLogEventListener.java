package io.streak.habitflow.domain.activitylog.listener;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {
    private final ActivityLogService activityLogService;

    @EventListener
    public void handleChanged(TaskChangedEvent taskChangedEvent) {
        ActivityLogRequest activityLogRequest = ActivityLogRequest.builder()
                .targetId(taskChangedEvent.targetId())
                .targetType(taskChangedEvent.targetType())
                .activityType(taskChangedEvent.activityType())
                .customMessage(taskChangedEvent.customMessage())
                .build();
        activityLogService.create(activityLogRequest, taskChangedEvent.memberId());
    }

}
