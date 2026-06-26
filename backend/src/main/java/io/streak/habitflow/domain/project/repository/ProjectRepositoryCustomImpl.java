package io.streak.habitflow.domain.project.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.project.dto.query.ProjectSearchSummaryQuery;
import io.streak.habitflow.domain.project.dto.query.ProjectSummaryQuery;
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
    public List<ProjectSearchSummaryQuery> searchByKeyword(String keyword, Long memberId, Pageable pageable) {
        return queryFactory
                .select(
                        Projections.constructor(
                                ProjectSearchSummaryQuery.class,
                                project.id,
                                project.name,
                                project.color,
                                project.sortOrder
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
    public List<ProjectSummaryQuery> findByMemberId(Long memberId) {
        return queryFactory
                .select(Projections.constructor(
                        ProjectSummaryQuery.class,
                        project.id,
                        project.name,
                        project.color,
                        task.count(),
                        project.sortOrder
                ))
                .from(project)
                .leftJoin(task).on(task.project.eq(project)
                        .and(task.completed.eq(false)))
                .innerJoin(projectMember).on(projectMember.project.eq(project).and(projectMember.member.id.eq(memberId)))
                .groupBy(project.id)
                .orderBy(project.sortOrder.asc())
                .fetch();
    }

    @Override
    public Long findMaxSortOrder(Long memberId, Long parentId) {
        return queryFactory
                .select(project.sortOrder.max().coalesce(0L))
                .from(projectMember)
                .innerJoin(projectMember.project,project)
                .where(
                        projectMember.member.id.eq(memberId),
                        parentId == null ? project.parent.isNull():project.parent.id.eq(parentId)
                )
                .fetchOne();
    }
}
