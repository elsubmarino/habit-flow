package io.streak.habitflow.domain.activitylog.repository;

import static io.streak.habitflow.domain.activitylog.entity.QActivityLog.activityLog;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.member.entity.QMember.member;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class ActivityLogRepositoryCustomImpl implements ActivityLogRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    @Override
    public List<ActivityLog> searchActivityLogs(ActivityLogSearchCondition activityLogSearchCondition) {
        return queryFactory
                .selectFrom(activityLog)
                .leftJoin(activityLog.member, member).fetchJoin()
                .where(
                        memberIdsIn(activityLogSearchCondition.getMemberIds()),
                        targetDateEq(activityLogSearchCondition.getTargetDate())
                )
                .orderBy(activityLog.createdAt.desc())
                .fetch();
    }

    @Override
    public List<ActivityLog> searchActivityLogsByCondition(Long activityLogId, Long memberId, Pageable pageable) {
        return queryFactory
                .selectFrom(activityLog)
                .leftJoin(activityLog.member, member).fetchJoin()
                .where(
                        activityLog.member.id.eq(memberId),
                        ltActivityLogId(activityLogId)
                )
                .orderBy(activityLog.id.desc())
                .limit(pageable.getPageSize()+1)
                .fetch();
    }

    private BooleanExpression memberIdsIn(List<Long> memberIds){
        return (memberIds == null || memberIds.isEmpty() ? null : activityLog.member.id.in(memberIds));
    }

    private BooleanExpression targetDateEq(LocalDate targetDate){
        if(targetDate == null) return null;
        return activityLog.createdAt.between(targetDate.atStartOfDay(),targetDate.atTime(23,59,59));
    }

    private BooleanExpression ltActivityLogId(Long activityLogId){
        if(activityLogId == null) return null;
        return activityLog.id.lt(activityLogId);
    }
}

