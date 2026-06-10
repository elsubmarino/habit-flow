package io.streak.habitflow.domain.activitylog.api;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogListResponse;
import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity-logs")
public class ActivityLogController {
    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<ScrollResponse<ActivityLogListResponse>> getActivityLogs(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                                   @RequestParam(value = "lastActivityLogId",required = false) Long lastActivityLogId) {
        return ResponseEntity.ok(activityLogService.getActivityLogs(lastActivityLogId,userPrincipal.getMemberId()));
    }

//    @GetMapping("/search")
//    public ResponseEntity<List<ActivityLogListResponse>> searchActivityLogs(@ModelAttribute ActivityLogSearchCondition activityLogSearchCondition
//                                                                        ) {
//        return ResponseEntity.ok(activityLogService.searchActivityLogs(activityLogSearchCondition));
//    }

}
