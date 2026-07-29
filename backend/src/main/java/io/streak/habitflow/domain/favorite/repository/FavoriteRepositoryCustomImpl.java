package io.streak.habitflow.domain.favorite.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.favorite.dto.query.FavoriteSummaryQuery;
import io.streak.habitflow.global.common.type.TargetType;
import lombok.RequiredArgsConstructor;

import java.util.*;

import static io.streak.habitflow.domain.favorite.entity.QFavorite.favorite;
import static io.streak.habitflow.domain.label.entity.QLabel.label;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTask.task;
import static io.streak.habitflow.domain.task.entity.QTaskLabel.taskLabel;

@RequiredArgsConstructor
public class FavoriteRepositoryCustomImpl implements FavoriteRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    @Override
    public List<FavoriteSummaryQuery> findFavoritesByMemberId(Long memberId) {

        List<Tuple> favorites = queryFactory
                .select(favorite.id,
                        favorite.publicId,
                        favorite.targetId,
                        favorite.targetType,
                        new CaseBuilder()
                                .when(favorite.targetType.eq(TargetType.PROJECT)).then(project.name)
                                .when(favorite.targetType.eq(TargetType.LABEL)).then(label.name)
                                .otherwise("알 수 없는 즐겨찾기").as("targetName"),
                        new CaseBuilder()
                                .when(favorite.targetType.eq(TargetType.PROJECT)).then(project.publicId)
                                .when(favorite.targetType.eq(TargetType.LABEL)).then(label.publicId)
                                .otherwise((UUID) null).as("targetPublicId")
                )
                .from(favorite)
                .leftJoin(project).on(favorite.targetType.eq(TargetType.PROJECT).and(project.id.eq(favorite.targetId)))
                .leftJoin(label).on(favorite.targetType.eq(TargetType.LABEL).and(label.id.eq(favorite.targetId)))
                .where(favorite.member.id.eq(memberId))
                .fetch();

        if(favorites.isEmpty()){
            return new ArrayList<>();
        }

        List<Long> projectIds = new ArrayList<>();
        List<Long> labelIds = new ArrayList<>();

        for(Tuple row: favorites){
            TargetType type = row.get(favorite.targetType);
            Long targetId = row.get(favorite.targetId);

            if(type == TargetType.PROJECT){
                projectIds.add(targetId);
            }else if(type == TargetType.LABEL){
                labelIds.add(targetId);
            }
        }

        Map<Long, Long> projectTaskCounts = new HashMap<>();
        if(!projectIds.isEmpty()){
            List<Tuple> pCounts = queryFactory
                    .select(task.project.id,task.count())
                    .from(task)
                    .where(
                            task.project.id.in(projectIds),
                            task.completed.eq(false)
                    )
                    .groupBy(task.project.id)
                    .fetch();

            for(Tuple t: pCounts){
                projectTaskCounts.put(t.get(task.project.id), t.get(1, Long.class));
            }
        }

        Map<Long,Long> labelTaskCounts = new HashMap<>();
        if(!labelIds.isEmpty()){
            List<Tuple> lCounts = queryFactory
                    .select()
                    .from(taskLabel)
                    .join(taskLabel.task,task)
                    .where(
                            taskLabel.label.id.in(labelIds),
                            task.completed.eq(false)
                    )
                    .groupBy(taskLabel.label.id)
                    .fetch();
            for(Tuple t: lCounts){
                labelTaskCounts.put(t.get(taskLabel.label.id), t.get(1, Long.class));
            }
        }

        List<FavoriteSummaryQuery> result = new ArrayList<>();
        for(Tuple row: favorites){
            Long id = row.get(favorite.id);
            Long targetId = row.get(favorite.targetId);
            TargetType type = row.get(favorite.targetType);
            String targetName = row.get(3, String.class);

            long count = 0L;

            if(type == TargetType.PROJECT){
                count = projectTaskCounts.getOrDefault(targetId, 0L);
            }else if(type == TargetType.LABEL){
                count = labelTaskCounts.getOrDefault(targetId, 0L);
            }

            UUID favoritePublicId = row.get(favorite.publicId);
            UUID targetPublicId = row.get(4,UUID.class);
            result.add(new FavoriteSummaryQuery(id, favoritePublicId,targetId,targetPublicId, targetName, type, count));
        }
        return result;
    }
}
