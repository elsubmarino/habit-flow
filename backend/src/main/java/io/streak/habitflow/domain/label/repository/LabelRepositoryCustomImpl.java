package io.streak.habitflow.domain.label.repository;

import static io.streak.habitflow.domain.label.entity.QLabel.label;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
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
                .fetch();
    }
}
