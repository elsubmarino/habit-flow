package io.streak.habitflow.domain.label.repository;

import static io.streak.habitflow.domain.label.entity.QLabel.label;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import lombok.RequiredArgsConstructor;
import java.util.List;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class LabelRepositoryCustomImpl implements LabelRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    @Override
    public List<LabelResponse> searchKeyword(String name, String email) {
        return queryFactory
                .select(Projections.fields(
                        LabelResponse.class,
                        label.name
                ))
                .from(label)
                .where(
                        label.member.email.eq(email),
                        label.name.contains(name)
                )
                .fetch();
    }
}
