package io.streak.habitflow.domain.activitylog.service;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogListResponse;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.domain.activitylog.repository.ActivityLogRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.common.dto.ScrollResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public void create(ActivityLogRequest activityLogRequest, Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);


        Long targetId = activityLogRequest.getTargetId();

        ActivityLog activityLog = ActivityLog.builder()
                .member(member)
                .activityType(activityLogRequest.getActivityType())
                .targetId(activityLogRequest.getTargetId())
                .customMessage(activityLogRequest.getCustomMessage())
                .build();
        activityLogRepository.save(activityLog);
    }

    public ScrollResponse<ActivityLogListResponse> getActivityLogs(Long lastActivityLogId, Long memberId, Pageable pageable) {

        List<ActivityLog> activityLogs = activityLogRepository.searchActivityLogsByCondition(lastActivityLogId, memberId, pageable);

        List<ActivityLogListResponse> activityLogListResponses = activityLogs.stream()
                .map(ActivityLogListResponse::from)
                .toList();

        return ScrollResponse.of(activityLogListResponses, pageable.getPageSize(), ActivityLogListResponse::id);
    }
