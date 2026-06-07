package io.streak.habitflow.domain.project.repository;

import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.project.entity.QProjectUser.projectUser;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import lombok.RequiredArgsConstructor;
import java.util.List;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ProjectListResponse> searchKeyword(String keyword, String email) {
        return queryFactory
                .select(
                        Projections.fields(
                                ProjectListResponse.class,
                                project.id,
                                project.name,
                                project.color
                        )
                ).from(project)
                .join(projectUser).on(projectUser.project.eq(project))
                .where(
                        projectUser.member.email.eq(email),
                        project.name.contains(keyword)
                )
                .fetch();
    }
}
