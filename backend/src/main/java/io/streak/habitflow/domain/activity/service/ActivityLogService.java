package io.streak.habitflow.domain.activity.service;

import io.streak.habitflow.domain.activity.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activity.dto.response.ActivityLogListResponse;
import io.streak.habitflow.domain.activity.dto.request.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activity.entity.ActivityLog;
import io.streak.habitflow.domain.activity.repository.ActivityLogRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
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
    private final TaskRepository taskRepository;

    @Transactional
    public void create(ActivityLogRequest activityLogRequest, UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 없습니다."));

        Long taskId = activityLogRequest.getTaskId();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(()-> new IllegalArgumentException("테스크가 존재하지 않습니다."));

        ActivityLog activityLog = ActivityLog.builder()
                .member(member)
                .activityType(activityLogRequest.getActivityType())
                .project(task.getProject())
                .build();
        activityLogRepository.save(activityLog);
    }

    public List<ActivityLogListResponse> getActivityLogs(UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 없습니다."));

        List<ActivityLog> activityLogs = activityLogRepository.findByMemberId(member.getId());
        return activityLogs.stream()
                .map(ActivityLogListResponse::from)
                .toList();
    }

    public List<ActivityLogListResponse> searchActivityLogs(ActivityLogSearchCondition activityLogSearchCondition){
        return  activityLogRepository.searchActivityLogs(activityLogSearchCondition).stream()
                .map(ActivityLogListResponse::from)
                .toList();
    }
}
