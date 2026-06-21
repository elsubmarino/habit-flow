package io.streak.habitflow.domain.activitylog.api;

import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogResponse;
import io.streak.habitflow.domain.activitylog.service.ActivityLogService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.streak.habitflow.global.common.RoutingId;
import io.streak.habitflow.global.common.constant.PageSizeConstants;
import io.streak.habitflow.global.util.HashidsProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity-logs")
public class ActivityLogController {
    private final ActivityLogService activityLogService;
    private final HashidsProvider hashidsProvider;

    @GetMapping
    @Operation(summary = "액티비티 로그 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "액티비티 로그 조회 성공")})
    public ResponseEntity<Slice<ActivityLogResponse.Summary>> getActivityLogs(@LoginMemberId Long loginMemberId,
                                                                              @RequestParam(value = "lastActivityLogId",required = false) RoutingId lastActivityLogId,
                                                                              @PageableDefault(size= PageSizeConstants.CURSOR_PAGING_NORMAL) Pageable pageable) {
        Long realLastActivityLogId = lastActivityLogId != null ? lastActivityLogId.value() : null;
        return ResponseEntity.ok(activityLogService.getActivityLogs(realLastActivityLogId,loginMemberId,pageable));
    }
}
