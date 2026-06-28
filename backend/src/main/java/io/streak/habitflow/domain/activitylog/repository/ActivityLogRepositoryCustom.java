package io.streak.habitflow.domain.activitylog.repository;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ActivityLogRepositoryCustom {
    List<ActivityLog> findActivityLogsBeforeId(Long lastActivityLogId, Long memberId, Pageable pageable, ActivityLogRequest.Search search,
                                               List<Long> memberIds, List<Long> targetIds);
}
