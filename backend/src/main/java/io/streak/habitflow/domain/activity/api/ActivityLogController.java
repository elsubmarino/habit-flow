package io.streak.habitflow.domain.activity.api;

import io.streak.habitflow.domain.activity.dto.response.ActivityLogListResponse;
import io.streak.habitflow.domain.activity.dto.request.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activity.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity-logs")
public class ActivityLogController {
    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<List<ActivityLogListResponse>> getActivityLogs(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(activityLogService.getActivityLogs(userDetails));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ActivityLogListResponse>> searchActivityLogs(@ModelAttribute ActivityLogSearchCondition activityLogSearchCondition
                                                                        ) {
        return ResponseEntity.ok(activityLogService.searchActivityLogs(activityLogSearchCondition));
    }

}
