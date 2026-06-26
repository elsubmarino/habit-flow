package io.streak.habitflow.domain.activitylog.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.activitylog.dto.request.ActivityLogRequest;
import io.streak.habitflow.domain.activitylog.entity.ActivityLog;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static io.streak.habitflow.domain.activitylog.entity.QActivityLog.activityLog;
import static io.streak.habitflow.domain.member.entity.QMember.member;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class ActivityLogRepositoryCustomImpl implements ActivityLogRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ActivityLog> searchActivityLogsByCondition(Long activityLogId, Long memberId, Pageable pageable, ActivityLogRequest.Search search,
                                                           List<Long> memberIds, List<Long> targetIds) {
        return queryFactory
                .selectFrom(activityLog)
                .leftJoin(activityLog.member, member).fetchJoin()
                .where(
                        activityLog.member.id.eq(memberId),
                        activityTypeIn(search.activityType()),
                        ltActivityLogId(activityLogId),
                        targetTypeEq(search.targetType(),targetIds),
                        targetDateEq(search.fromDate(),search.toDate())
                )
                .orderBy(activityLog.id.desc())
                .limit(pageable.getPageSize()+1)
                .fetch();
    }

    private BooleanExpression memberIdIn(List<Long> memberIds) {
        if(memberIds == null || memberIds.isEmpty()){
            return null;
        }
        return activityLog.member.id.in(memberIds);
    }

    private BooleanExpression activityTypeIn(List<ActivityType> activityTypes) {
        if(activityTypes == null) return null;
        return activityLog.activityType.in(activityTypes);
    }

    private BooleanExpression targetTypeEq(TargetType targetType, List<Long> targetIds){
        if(targetType == null) return null;
        if (targetType == TargetType.PROJECT) {
            return activityLog.targetType.eq(TargetType.PROJECT)
                    .and(activityLog.targetId.in(targetIds));
        }
        return null;
    }

    private BooleanExpression targetDateEq(LocalDate fromDate, LocalDate toDate){
        LocalDateTime fromDateTime;
        LocalDateTime toDateTime;
        if(fromDate == null) return null;
        fromDateTime = fromDate.atTime(LocalTime.MIN);
        if(toDate == null){
            toDateTime = fromDate.atTime(LocalTime.MAX);
        }else{
            toDateTime = toDate.atTime(LocalTime.MAX);
        }
        return activityLog.createdAt.between(fromDateTime,toDateTime);
    }

    private BooleanExpression ltActivityLogId(Long activityLogId){
        if(activityLogId == null) return null;
        return activityLog.id.lt(activityLogId);
    }
}

