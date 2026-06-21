package io.streak.habitflow.domain.favorite.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.favorite.dto.query.FavoriteSummaryQuery;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.task.entity.QTask;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.streak.habitflow.domain.favorite.entity.QFavorite.favorite;
import static io.streak.habitflow.domain.label.entity.QLabel.label;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTask.task;
import static io.streak.habitflow.domain.task.entity.QTaskLabel.taskLabel;

@RequiredArgsConstructor
public class FavoriteRepositoryCustomImpl implements FavoriteRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    @Override
    public List<FavoriteSummaryQuery> findByMemberId(Long memberId) {
        QTask subTask = new QTask("subTask");

        return queryFactory
                .select(Projections.constructor(FavoriteSummaryQuery.class,
                        favorite.id,
                        favorite.targetId,
                        new CaseBuilder()
                                .when(favorite.targetType.eq(TargetType.PROJECT)).then(project.name)
                                .when(favorite.targetType.eq(TargetType.LABEL)).then(label.name)
                                .otherwise("알 수 없는 즐겨찾기").as("targetName"),
                        favorite.targetType,
                        new CaseBuilder()
                                .when(favorite.targetType.eq(TargetType.PROJECT)).then(
                                        JPAExpressions.select(task.count())
                                                .from(task)
                                                .where(task.project.id.eq(favorite.targetId),
                                                        task.completed.eq(false)
                                                )
                                )
                                .when(favorite.targetType.eq(TargetType.LABEL)).then(
                                        JPAExpressions.select(taskLabel.count())
                                                .from(taskLabel)
                                                .join(taskLabel.task,subTask)
                                                .where(taskLabel.label.id.eq(favorite.targetId),
                                                        subTask.completed.eq(false)
                                                )
                                )
                                .otherwise(0L).as("targetCount")
                        ))
                .from(favorite)
                .leftJoin(project).on(favorite.targetType.eq(TargetType.PROJECT).and(project.id.eq(favorite.targetId)))
                .leftJoin(label).on(favorite.targetType.eq(TargetType.LABEL).and(label.id.eq(favorite.targetId)))
                .where(favorite.member.id.eq(memberId))
                .fetch();

    }
}
