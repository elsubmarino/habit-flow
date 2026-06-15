package io.streak.habitflow.domain.label.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.label.dto.query.LabelListQuery;
import io.streak.habitflow.domain.label.entity.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static io.streak.habitflow.domain.label.entity.QLabel.label;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class LabelRepositoryCustomImpl implements LabelRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    @Override
    public List<LabelListQuery> searchKeyword(String name, Long memberId, Pageable pageable) {
        return queryFactory
                .select(Projections.fields(
                        LabelListQuery.class,
                        label.id,
                        label.name,
                        label.color,
                        label.sortOrder
                ))
                .from(label)
                .where(
                        label.member.id.eq(memberId),
                        label.name.contains(name)
                )
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Label> searchLabelsByCondition(Long labelId, Long memberId, Pageable pageable) {
        return queryFactory
                .selectFrom(label)
                .where(
                        label.member.id.eq(memberId),
                        ltLabelId(labelId)
                )
                .orderBy(label.id.desc())
                .limit(pageable.getPageSize()+1)
                .fetch();
    }

    private BooleanExpression ltLabelId(Long labelId) {
        if(labelId == null) return null;
        return label.id.lt(labelId);
    }
}
