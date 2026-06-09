package io.streak.habitflow.domain.label.repository;

import static io.streak.habitflow.domain.label.entity.QLabel.label;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.label.dto.request.LabelSearchRequest;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import lombok.RequiredArgsConstructor;
import java.util.List;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class LabelRepositoryCustomImpl implements LabelRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    @Override
    public List<LabelListResponse> searchKeyword(String name, String email) {
        return queryFactory
                .select(Projections.fields(
                        LabelListResponse.class,
                        label.id,
                        label.name,
                        label.color,
                        label.sortOrder
                ))
                .from(label)
                .where(
                        label.member.email.eq(email),
                        label.name.contains(name)
                )
                .limit(5)
                .fetch();
    }

    @Override
    public List<Label> searchLabelsByCondition(Long labelId, Long memberId) {
        int pageSize = 20;
        return queryFactory
                .selectFrom(label)
                .where(
                        label.member.id.eq(memberId),
                        ltLabelId(labelId)
                )
                .orderBy(label.id.desc())
                .limit(pageSize+1)
                .fetch();
    }

    private BooleanExpression ltLabelId(Long labelId) {
        if(labelId == null) return null;
        return label.id.lt(labelId);
    }
}
