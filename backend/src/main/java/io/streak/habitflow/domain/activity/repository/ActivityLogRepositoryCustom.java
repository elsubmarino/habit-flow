package io.streak.habitflow.domain.activity.repository;

import io.streak.habitflow.domain.activity.dto.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activity.entity.ActivityLog;

import java.util.List;

public interface ActivityLogRepositoryCustom {
    List<ActivityLog> searchActivityLogs(ActivityLogSearchCondition activityLogSearchCondition);

}
