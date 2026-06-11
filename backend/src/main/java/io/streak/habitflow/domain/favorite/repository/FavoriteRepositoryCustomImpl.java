package io.streak.habitflow.domain.favorite.repository;

import com.querydsl.core.types.ExpressionUtils;
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
import io.streak.habitflow.domain.task.entity.QTaskInstance;
import io.streak.habitflow.domain.task.entity.QTaskLabel;
import io.streak.habitflow.domain.task.entity.QTaskMaster;
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

        QTaskMaster pTask = new QTaskMaster("pTask");
        QTaskInstance pInstance = new QTaskInstance("pInstance");

        QTaskLabel lTaskLabel = new QTaskLabel("lTaskLabel");
        QTaskMaster lTask = new QTaskMaster("lMaster");
        QTaskInstance lInstance = new QTaskInstance("lInstance");

        return queryFactory
                .select(Projections.fields(FavoriteListResponse.class,
                        favorite.id,
                        favorite.targetType,
                        favorite.targetId,
                        new CaseBuilder()
                                .when(favorite.targetType.eq(TargetType.PROJECT)).then(
                                        JPAExpressions.select(pInstance.count())
                                                .from(pInstance)
                                                .join(pInstance.taskMaster, pTask)
                                                .where(pTask.project.id.eq(favorite.targetId),
                                                        pInstance.isCompleted.eq(false)
                                                )
                                )
                                .when(favorite.targetType.eq(TargetType.LABEL)).then(
                                        JPAExpressions.select(lTaskLabel.count())
                                                .from(lTaskLabel)
                                                .join(lTaskLabel.taskMaster,lTask)
                                                .join(lTask.taskInstances, lInstance)
                                                .where(lTaskLabel.label.id.eq(favorite.targetId),
                                                        lInstance.isCompleted.eq(false)
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
