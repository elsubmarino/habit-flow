package io.streak.habitflow.domain.favorite.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.favorite.dto.response.FavoriteListResponse;
import io.streak.habitflow.domain.favorite.entity.QFavorite;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.label.entity.QLabel;
import io.streak.habitflow.domain.member.entity.QMember;
import io.streak.habitflow.domain.project.entity.QProject;
import io.streak.habitflow.domain.task.entity.QTaskLabel;
import io.streak.habitflow.domain.task.entity.QTask;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class FavoriteRepositoryCustomImpl implements FavoriteRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    @Override
    public List<FavoriteListResponse> findByMemberId(Long memberId) {
        QFavorite favorite = QFavorite.favorite;
        QMember member = QMember.member;
        QProject project = QProject.project;
        QLabel label = QLabel.label;

        QTask pTask = new QTask("pTask");

        QTaskLabel lTaskLabel = new QTaskLabel("lTaskLabel");
        QTask lTask = new QTask("lMaster");

        return queryFactory
                .select(Projections.fields(FavoriteListResponse.class,
                        favorite.id,
                        favorite.targetType,
                        favorite.targetId,
                        new CaseBuilder()
                                .when(favorite.targetType.eq(TargetType.PROJECT)).then(
                                        JPAExpressions.select(pTask.count())
                                                .from(pTask)
                                                .where(pTask.project.id.eq(favorite.targetId),
                                                        pTask.completed.eq(false)
                                                )
                                )
                                .when(favorite.targetType.eq(TargetType.LABEL)).then(
                                        JPAExpressions.select(lTaskLabel.count())
                                                .from(lTaskLabel)
                                                .join(lTaskLabel.task,lTask)
                                                .where(lTaskLabel.label.id.eq(favorite.targetId),
                                                        lTask.completed.eq(false)
                                                )
                                )
                                .otherwise(0L).as("targetCount"),
                        new CaseBuilder()
                                .when(favorite.targetType.eq(TargetType.PROJECT)).then(project.name)
                                .when(favorite.targetType.eq(TargetType.LABEL)).then(label.name)
                                .otherwise("알 수 없는 즐겨찾기").as("targetName")
                        ))
                .from(favorite)
                .leftJoin(favorite.member,member)
                .leftJoin(project).on(favorite.targetType.eq(TargetType.PROJECT).and(project.id.eq(favorite.targetId)))
                .leftJoin(label).on(favorite.targetType.eq(TargetType.LABEL).and(label.id.eq(favorite.targetId)))
                .where(favorite.member.id.eq(memberId))
                .fetch();

    }
}
