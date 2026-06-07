package io.streak.habitflow.domain.activity.repository;

import static io.streak.habitflow.domain.activity.entity.QActivityLog.activityLog;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.member.entity.QMember.member;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.activity.dto.request.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activity.entity.ActivityLog;
import lombok.RequiredArgsConstructor;
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
                .leftJoin(activityLog.project, project).fetchJoin()
                .leftJoin(activityLog.member, member).fetchJoin()
                .where(
                        projectIdsIn(activityLogSearchCondition.getProjectIds()),
                        memberIdsIn(activityLogSearchCondition.getUserIds()),
                        targetDateEq(activityLogSearchCondition.getTargetDate())
                )
                .orderBy(activityLog.createdAt.desc())
                .fetch();
    }

    private BooleanExpression projectIdsIn(List<Long> projectIds){
        return (projectIds == null || projectIds.isEmpty() ? null : activityLog.project.id.in(projectIds));
    }

    private BooleanExpression memberIdsIn(List<Long> memberIds){
        return (memberIds == null || memberIds.isEmpty() ? null : activityLog.member.id.in(memberIds));
    }

    private BooleanExpression targetDateEq(LocalDate targetDate){
        if(targetDate == null) return null;
        return activityLog.createdAt.between(targetDate.atStartOfDay(),targetDate.atTime(23,59,59));
    }
}

