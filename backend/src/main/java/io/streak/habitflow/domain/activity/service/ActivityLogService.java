package io.streak.habitflow.domain.activity.service;

import io.streak.habitflow.domain.activity.dto.ActivityLogRequest;
import io.streak.habitflow.domain.activity.dto.ActivityLogResponse;
import io.streak.habitflow.domain.activity.dto.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activity.entity.ActivityLog;
import io.streak.habitflow.domain.activity.repository.ActivityLogRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;

    /**
     * 액티비티 로그 생성
     * @param activityLogRequest 요청된 액티비티 로그 요청 DTO 정보
     * @param userDetails 인증된 사용자 정보
     */
    @Transactional
    @SuppressWarnings("unused")
    public void create(ActivityLogRequest activityLogRequest, UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 없습니다."));

        ActivityLog activityLog = ActivityLog.builder()
                .member(member)
                .activityType(activityLogRequest.getActivityType())
                .build();
        activityLogRepository.save(activityLog);
    }

    /**
     * 액티비티 로그 조회
     * @param userDetails 요청된 사용자 정보
     * @return 조회된 다건의 액티비티 로그 응답 정보
     */
    public List<ActivityLogResponse> getActivityLogs(UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 없습니다."));

        List<ActivityLog> activityLogs = activityLogRepository.findByMemberId(member.getId());
        return activityLogs.stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    public List<ActivityLogResponse> searchActivityLogs(ActivityLogSearchCondition activityLogSearchCondition){
        return  activityLogRepository.searchActivityLogs(activityLogSearchCondition).stream()
                .map(ActivityLogResponse::from)
                .toList();
    }
}
