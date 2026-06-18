package io.streak.habitflow.domain.activitylog.service;

import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogResponse;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.domain.activitylog.mapper.ActivityLogMapper;
import io.streak.habitflow.domain.activitylog.repository.ActivityLogRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;
    private final ActivityLogMapper activityLogMapper;

    @Transactional
    public void create(TaskChangedEvent taskChangedEvent) {
        Member owner = memberRepository.getReferenceById(taskChangedEvent.memberId());

        ActivityLog activityLog = ActivityLog.builder()
                .member(owner)
                .actor(owner)
                .targetId(taskChangedEvent.targetId())
                .targetType(taskChangedEvent.targetType())
                .activityType(taskChangedEvent.activityType())
                .targetName(taskChangedEvent.targetName())
                .changes(taskChangedEvent.changes())
                .build();

        activityLogRepository.save(activityLog);
    }

    public Slice<ActivityLogResponse.Summary> getActivityLogs(Long lastActivityLogId, Long memberId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<ActivityLog> activityLogs = activityLogRepository.searchActivityLogsByCondition(lastActivityLogId, memberId, pageable);

        boolean hasNext = false;
        if(activityLogs.size() > pageSize){
            activityLogs.remove(pageSize);
            hasNext = true;
        }

        List<ActivityLogResponse.Summary> activityLogResponses = activityLogs.stream()
                .map(activityLogMapper::toSummary)
                .toList();

        return new SliceImpl<>(activityLogResponses, pageable ,hasNext);
    }
}
