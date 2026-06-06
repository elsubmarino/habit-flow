package io.streak.habitflow.domain.label.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.label.dto.LabelResponse;
import static io.streak.habitflow.domain.label.entity.QLabel.label;

import io.streak.habitflow.domain.label.entity.QLabel;
import io.streak.habitflow.domain.project.entity.QProject;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
