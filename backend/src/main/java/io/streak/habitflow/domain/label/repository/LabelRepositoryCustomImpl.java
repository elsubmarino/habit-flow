package io.streak.habitflow.domain.label.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.label.dto.query.LabelSummaryQuery;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.streak.habitflow.domain.label.entity.QLabel.label;
import static io.streak.habitflow.domain.task.entity.QTaskLabel.taskLabel;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class LabelRepositoryCustomImpl implements LabelRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final HashidsProvider hashidsProvider;

    @Override
    public List<LabelSummaryQuery> searchByKeyword(String keyword, Long memberId, Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(
                        LabelSummaryQuery.class,
                        label.id,
                        label.publicId,
                        label.name,
                        label.color,
                        label.sortOrder
                ))
                .from(label)
                .where(
                        label.member.id.eq(memberId),
                        label.name.contains(keyword)
                )
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Label> findLabelsByMemberWithCursor(Long lastLabelId, Long memberId, Pageable pageable) {
        return queryFactory
                .selectFrom(label)
                .where(
                        label.member.id.eq(memberId),
                        ltLabelId(lastLabelId)
                )
                .orderBy(label.id.desc())
                .limit(pageable.getPageSize()+1)
                .fetch();
    }

    private BooleanExpression ltLabelId(Long labelId) {
        if(labelId == null) return null;
        return label.id.lt(labelId);
    }

    @Override
    public Map<Long, List<LabelResponse.Summary>> findLabelSummariesByTaskIds(List<Long> taskIds) {
        if(taskIds == null || taskIds.isEmpty()){
            return Collections.emptyMap();
        }

        List<Tuple> results = queryFactory
                .select(taskLabel.task.id,
                        label.id,
                        label.publicId,
                        label.name,
                        label.color,
                        label.sortOrder)
                .from(taskLabel)
                .join(taskLabel.label,label)
                .where(taskLabel.task.id.in(taskIds))
                .fetch();

        return results.stream()
                .collect(Collectors.groupingBy(row -> row.get(taskLabel.task.id),
                        Collectors.mapping(row -> {
                                    if (row.get(label.id) == null) return null;
                                    Long dbSortOrder = row.get(label.sortOrder);
                                    long safeSortOrder = (dbSortOrder != null) ? dbSortOrder : 0L;

                                    return LabelResponse.Summary.builder()
                                            .id(Objects.requireNonNull(row.get(label.publicId)).toString())
                                            .name(row.get(label.name))
                                            .color(row.get(label.color))
                                            .sortOrder(safeSortOrder)
                                            .build();
                                },
                                Collectors.filtering(Objects::nonNull, Collectors.toList()))
                        )
                );

    }

    @Override
    public List<LabelSummaryQuery> findLabelSummariesByTaskId(Long taskId) {
        return queryFactory
                .select(Projections.constructor(LabelSummaryQuery.class,
                        label.id,
                        label.name,
                        label.color,
                        label.sortOrder))
                .from(taskLabel)
                .join(taskLabel.label,label)
                .where(taskLabel.task.id.eq(taskId))
                .fetch();
    }
}
