package io.streak.habitflow.domain.activity.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.activity.dto.ActivityLogSearchCondition;
import io.streak.habitflow.domain.activity.entity.ActivityLog;
import lombok.RequiredArgsConstructor;

import static io.streak.habitflow.domain.activity.entity.QActivityLog.activityLog;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.member.entity.QMember.member;

import java.util.List;

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
                        memberIdsIn(activityLogSearchCondition.getUserIds())
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
}

