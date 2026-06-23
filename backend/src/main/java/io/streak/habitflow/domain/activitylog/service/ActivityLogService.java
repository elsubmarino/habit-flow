package io.streak.habitflow.domain.activitylog.service;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogResponse;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.domain.activitylog.mapper.ActivityLogMapper;
import io.streak.habitflow.domain.activitylog.repository.ActivityLogRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.global.util.HashidsProvider;
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
    private final HashidsProvider hashidsProvider;

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

    public Slice<ActivityLogResponse.Summary> getActivityLogs(Long lastActivityLogId, Long memberId, Pageable pageable, ActivityLogRequest.Search search) {
        int pageSize = pageable.getPageSize();
        List<Long> memberIds = List.of();
        List<Long> targetIds = List.of();
        if(search.memberIds() != null){
            memberIds = search.memberIds().stream()
                    .map(hashidsProvider::decode).toList();
        }
        
        if(search.targetIds() != null){
            targetIds = search.targetIds().stream()
                    .map(hashidsProvider::decode).toList();   
        }

        List<ActivityLog> activityLogs = activityLogRepository.searchActivityLogsByCondition(lastActivityLogId, memberId, pageable,search,
                memberIds,targetIds);

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
