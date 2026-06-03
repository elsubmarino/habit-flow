package io.streak.habitflow.domain.activity.repository;

import io.streak.habitflow.domain.activity.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog,Long> {
}
