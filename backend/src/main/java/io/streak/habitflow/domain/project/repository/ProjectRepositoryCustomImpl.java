package io.streak.habitflow.domain.project.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.project.dto.query.ProjectListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.project.entity.QProjectMember.projectMember;
import static io.streak.habitflow.domain.task.entity.QTask.task;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ProjectListQuery> searchKeyword(String keyword, Long memberId, Pageable pageable) {
        return queryFactory
                .select(
                        Projections.fields(
                                ProjectListQuery.class,
                                project.id,
                                project.name,
                                project.color
                        )
                ).from(project)
                .join(projectMember).on(projectMember.project.eq(project))
                .where(
                        projectMember.member.id.eq(memberId),
                        project.name.contains(keyword)
                )
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<ProjectListQuery> findByMemberId(Long memberId) {
        return queryFactory
                .select(Projections.constructor(
                        ProjectListQuery.class,
                        project.id,
                        project.name,
                        project.color,
                        task.count()
                ))
                .from(project)
                .leftJoin(task).on(task.project.eq(project)
                        //TODO
                        //.and(task.completed.eq(false))
                )
                .innerJoin(projectMember).on(projectMember.project.eq(project).and(projectMember.member.id.eq(memberId)))
                .groupBy(project.id)
                .fetch();
    }
}
