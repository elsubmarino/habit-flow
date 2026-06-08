package io.streak.habitflow.domain.activitylog.repository;

import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog,Long>, ActivityLogRepositoryCustom {
    List<ActivityLog> findByMemberId(Long memberId);
}
