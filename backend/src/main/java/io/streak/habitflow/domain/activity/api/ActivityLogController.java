package io.streak.habitflow.domain.activity.api;

import io.streak.habitflow.domain.activity.dto.ActivityLogRequest;
import io.streak.habitflow.domain.activity.dto.ActivityLogResponse;
import io.streak.habitflow.domain.activity.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/log")
public class ActivityLogController {
    private final ActivityLogService activityLogService;

    /**
     * 액티비티 로그 조회
     *
     * @param activityLogRequest
     * @param userDetails
     * @return
     */
    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> getActivityLogs(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(activityLogService.getActivityLogs(userDetails));
    }
}
