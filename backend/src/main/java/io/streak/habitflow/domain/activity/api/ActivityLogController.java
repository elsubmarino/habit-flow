package io.streak.habitflow.domain.activity.api;

import io.streak.habitflow.domain.activity.dto.ActivityLogRequest;
import io.streak.habitflow.domain.activity.dto.ActivityLogResponse;
import io.streak.habitflow.domain.activity.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> getActivityLogs(@RequestBody ActivityLogRequest activityLogRequest, @AuthenticationPrincipal UserDetails userDetails) {
        List<ActivityLogResponse> activityLogResponses =  activityLogService.getActivityLogs(activityLogRequest,userDetails);
        return ResponseEntity.ok(activityLogResponses);
    }
}
