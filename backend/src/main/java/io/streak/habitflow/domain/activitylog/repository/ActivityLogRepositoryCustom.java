package io.streak.habitflow.domain.activitylog.repository;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ActivityLogRepositoryCustom {
    List<ActivityLog> searchActivityLogs(ActivityLogSearchCondition activityLogSearchCondition);
    List<ActivityLog> searchActivityLogsByCondition(Long activityLogId, Long memberId, Pageable pageable);
}
