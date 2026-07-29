package io.streak.habitflow.domain.activitylog.service;

import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.dto.response.ActivityLogResponse;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.domain.activitylog.event.ActivityRecordedEvent;
import io.streak.habitflow.domain.activitylog.mapper.ActivityLogMapper;
import io.streak.habitflow.domain.activitylog.repository.ActivityLogRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;
    private final ActivityLogMapper activityLogMapper;
    private final ProjectRepository projectRepository;

    @Transactional
    public void recordActivity(ActivityRecordedEvent activityRecordedEvent) {
        Member owner = memberRepository.getReferenceById(activityRecordedEvent.memberId());

        ActivityLog activityLog = ActivityLog.builder()
                .member(owner)
                .actor(owner)
                .targetId(activityRecordedEvent.targetId())
                .targetType(activityRecordedEvent.targetType())
                .activityType(activityRecordedEvent.activityType())
                .targetName(activityRecordedEvent.targetName())
                .changes(activityRecordedEvent.changes())
                .build();

        activityLogRepository.save(activityLog);
    }

    public Slice<ActivityLogResponse.Summary> getActivityLogs(UUID lastActivityLogPublicId, Long memberId, Pageable pageable, ActivityLogRequest.Search search) {
        int pageSize = pageable.getPageSize();
        Long lastActivityLogId = lastActivityLogPublicId != null ? activityLogRepository.findByPublicId(lastActivityLogPublicId)
                .map(ActivityLog::getId)
                .orElse(null):null;

        List<Long> memberIds = List.of();
        if(search.memberIds() != null){
            memberIds = memberRepository.findAllByPublicIdIn(search.memberIds())
                    .stream()
                    .map(Member::getId).toList();
        }

        List<Long> targetIds = List.of();
        if(search.targetIds() != null){
            targetIds = projectRepository.findAllByPublicId(search.memberIds())
                    .stream()
                    .map(Project::getId).toList();
        }

        List<ActivityLog> activityLogs = activityLogRepository.findActivityLogsBeforeId(lastActivityLogId, memberId, pageable,search,
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
