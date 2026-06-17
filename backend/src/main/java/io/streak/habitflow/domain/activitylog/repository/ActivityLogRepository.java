package io.streak.habitflow.domain.activitylog.repository;

import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog,Long>, ActivityLogRepositoryCustom {
}
