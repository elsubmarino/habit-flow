package io.streak.habitflow.domain.activitylog.api;

import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogListResponse;
import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity-logs")
public class ActivityLogController {
    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<ScrollResponse<ActivityLogListResponse>> getActivityLogs(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                                   @RequestParam(value = "lastActivityLogId",required = false) Long lastActivityLogId,
                                                                                   @PageableDefault(size=20)Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivityLogs(lastActivityLogId,userPrincipal.getMemberId(),pageable));
    }

//    @GetMapping("/search")
//    public ResponseEntity<List<ActivityLogListResponse>> searchActivityLogs(@ModelAttribute ActivityLogSearchCondition activityLogSearchCondition
//                                                                        ) {
//        return ResponseEntity.ok(activityLogService.searchActivityLogs(activityLogSearchCondition));
//    }

}
