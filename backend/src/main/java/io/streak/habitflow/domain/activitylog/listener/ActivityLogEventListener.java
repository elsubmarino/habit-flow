package io.streak.habitflow.domain.activitylog.listener;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {
    private final ActivityLogService activityLogService;

    @Async("activityLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChanged(TaskChangedEvent taskChangedEvent) {
        log.info("[Async Event Check] 현재 쓰레드 : {} -> 로그 저장 시작", Thread.currentThread().getName());
        ActivityLogRequest activityLogRequest = ActivityLogRequest.builder()
                .targetId(taskChangedEvent.targetId())
                .targetType(taskChangedEvent.targetType())
                .activityType(taskChangedEvent.activityType())
                .customMessage(taskChangedEvent.customMessage())
                .build();
        activityLogService.create(activityLogRequest, taskChangedEvent.memberId());
    }

}
