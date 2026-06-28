package io.streak.habitflow.domain.activitylog.listener;

import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.domain.task.event.ActivityRecordedEvent;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {
    private final ActivityLogService activityLogService;
    private final TaskRepository taskRepository;

    @Async("activityLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivityRecorded(ActivityRecordedEvent activityRecordedEvent) {
        log.info("[Async Event Check] 현재 쓰레드 : {} -> 로그 저장 시작", Thread.currentThread().getName());

        activityLogService.recordActivity(activityRecordedEvent);
    }

}
