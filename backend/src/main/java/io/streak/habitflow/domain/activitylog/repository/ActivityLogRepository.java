package io.streak.habitflow.domain.activitylog.repository;

import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog,Long>, ActivityLogRepositoryCustom {
    Optional<ActivityLog> findByPublicId(UUID publicId);
}
