package io.streak.habitflow.domain.project.repository;

import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.project.entity.QProjectMember.projectMember;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.project.dto.response.ProjectListResponse;
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
                .join(projectMember).on(projectMember.project.eq(project))
                .where(
                        projectMember.member.email.eq(email),
                        project.name.contains(keyword)
                )
                .fetch();
    }
}
