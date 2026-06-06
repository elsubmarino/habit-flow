package io.streak.habitflow.domain.activity.api;

import io.streak.habitflow.domain.activity.dto.ActivityLogResponse;
import io.streak.habitflow.domain.activity.dto.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activity.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/log")
public class ActivityLogController {
    private final ActivityLogService activityLogService;

    /**
     * 액티비티 로그 조회
     *
     * @param userDetails
     * @return
     */
    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> getActivityLogs(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(activityLogService.getActivityLogs(userDetails));
    }

    /**
     * 액티비티 로그 검색
     * @param activityLogSearchCondition
     * @param userDetails
     * @return
     */
    @GetMapping("/search")
    public ResponseEntity<List<ActivityLogResponse>> searchActivityLogs(@ModelAttribute ActivityLogSearchCondition activityLogSearchCondition, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(activityLogService.searchActivityLogs(activityLogSearchCondition));
    }

}
