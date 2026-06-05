package io.streak.habitflow.domain.activity.service;

import io.streak.habitflow.domain.activity.dto.ActivityLogRequest;
import io.streak.habitflow.domain.activity.dto.ActivityLogResponse;
import io.streak.habitflow.domain.activity.entity.ActivityLog;
import io.streak.habitflow.domain.activity.repository.ActivityLogRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;

    /**
     * 액티비티 로그 생성
     * @param activityLogRequest
     * @param userDetails
     */
    @Transactional
    public void create(ActivityLogRequest activityLogRequest, UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 없습니다."));

        ActivityLog activityLog = ActivityLog.builder()
                .member(member)
                .activityType(activityLogRequest.getActivityType())
                .targetType(activityLogRequest.getTargetType())
                .targetId(activityLogRequest.getTargetId())
                .build();
        activityLogRepository.save(activityLog);
    }

    /**
     * 액티비티 로그 조회
     * @param activityLogRequest
     * @param userDetails
     * @return
     */
    public List<ActivityLogResponse> getActivityLogs(ActivityLogRequest activityLogRequest, UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 없습니다."));

        List<ActivityLog> activityLogs = activityLogRepository.findByUserId(member.getId());
        return activityLogs.stream()
                .map(ActivityLogResponse::from)
                .collect(Collectors.toList());
    }
}
