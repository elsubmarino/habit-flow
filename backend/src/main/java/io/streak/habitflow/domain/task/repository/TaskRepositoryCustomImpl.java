package io.streak.habitflow.domain.task.repository;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.task.dto.query.TaskListQuery;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.QTask;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.streak.habitflow.domain.comment.entity.QComment.comment;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTask.task;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Task> searchTasks(TaskRequest.Update request, String email) {
        return queryFactory
                .selectFrom(task)
                .where(
                        nameContains(request.name()),
                        descriptionContains(request.description()),
                        task.member.email.eq(email)
                )
                .orderBy(task.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<Task> searchTaskInfo(Long taskId) {
        Task result = queryFactory
                .selectFrom(task)
                .leftJoin(task.project, project).fetchJoin()
                .where(task.id.eq(taskId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? task.name.contains(name) : null;
    }

    private BooleanExpression descriptionContains(String description) {
        return StringUtils.hasText(description) ? task.description.contains(description) : null;
    }

    @Override
    public List<TaskResponse> searchKeyword(String keyword, Long memberId, Pageable pageable) {
        return
                queryFactory
                        .select(Projections.fields(
                                TaskResponse.class,
                                task.id,
                                task.name
                        ))
                        .from(task)
                        .where(
                                task.name.contains(keyword),
                                task.member.id.eq(memberId)
                        )
                        .limit(pageable.getPageSize())
                        .fetch();
    }

    @Override
    public List<TaskListQuery> findTasksByProject(Long projectId, Long memberId, Pageable pageable) {
        List<Long> ids = queryFactory
                .select(task.id)
                .from(task)
                .where(task.member.id.eq(memberId),
                        task.project.id.eq(projectId),
                        task.completed.eq(false)
                )
                .orderBy(
                        task.dueDate.asc(),
                        task.taskPriorityType.asc(),
                        task.sortOrder.asc(),
                        task.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        QTask subTask = new QTask("subTask");

        return queryFactory
                .select(
                        Projections.fields(
                                TaskListQuery.class,
                                task.id,
                                task.name,
                                task.description,
                                task.taskPriorityType,
                                task.dueDate,
                                task.hasTime,
                                task.sortOrder,
                                task.project.name.as("projectName"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTask.count())
                                                .from(subTask)
                                                .where(subTask.parent.eq(task)), "countSubTasks"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTask.count())
                                                .from(subTask)
                                                .where(subTask.parent.eq(task)
                                                        .and(subTask.completed.eq(true))), "countSubTasksCompleted"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(comment.count())
                                                .from(comment)
                                                .where(comment.task.eq(task)), "countComments")
                        )
                )
                .from(task)
                .leftJoin(task.project, project)
                .where(
                        task.id.in(ids)
                )
                .orderBy(
                        task.dueDate.asc(),
                        task.taskPriorityType.asc(),
                        task.sortOrder.asc(),
                        task.id.desc()
                )
                .fetch();
    }

    @Override
    public List<TaskListQuery> searchTasksByCondition(TaskRequest.SearchCondition searchCondition, Long memberId) {

        List<Long> ids = queryFactory
                .select(task.id)
                .from(task)
                .where(task.member.id.eq(memberId),
                        filterTypeEq(searchCondition.taskFilterType()),
                        task.completed.eq(false)
                )
                .orderBy(
                        task.dueDate.asc(),
                        task.taskPriorityType.asc(),
                        task.sortOrder.asc(),
                        task.id.desc()
                )
                .fetch();
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        QTask subTask = new QTask("subTask");

        return queryFactory
                .select(
                        Projections.fields(
                                TaskListQuery.class,
                                task.id,
                                task.name,
                                task.description,
                                task.taskPriorityType,
                                task.dueDate,
                                task.hasTime,
                                task.sortOrder,
                                task.project.name.as("projectName"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTask.count())
                                                .from(subTask)
                                                .where(subTask.parent.eq(task)), "countSubTasks"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTask.count())
                                                .from(subTask)
                                                .where(subTask.parent.eq(task)
                                                        .and(subTask.completed.eq(true))), "countSubTasksCompleted"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(comment.count())
                                                .from(comment)
                                                .where(comment.task.eq(task)), "countComments")
                        )
                )
                .from(task)
                .leftJoin(task.project, project)
                .where(
                        task.id.in(ids)
                )
                .orderBy(
                        task.dueDate.asc(),
                        task.taskPriorityType.asc(),
                        task.sortOrder.asc(),
                        task.id.desc()
                )
                .fetch();
    }

    private BooleanExpression filterTypeEq(TaskFilterType taskFilterType) {
        if (taskFilterType == null) return null;
        LocalDateTime localDate = LocalDateTime.now();
        if (TaskFilterType.TODAY == taskFilterType) {
            return task.dueDate.loe(localDate);
        } else if (TaskFilterType.UPCOMING == taskFilterType) {
            return task.dueDate.loe(localDate.plusYears(2));
        } else if (TaskFilterType.INBOX == taskFilterType) {
            return task.project.isNull();
        }
        return null;
    }

    private BooleanExpression ltTaskId(Long taskId) {
        if (taskId == null) return null;
        return task.id.lt(taskId);
    }

    @Override
    public long countTasksByCondition(TaskFilterType taskFilterType, Long memberId) {
        Long totalCount = queryFactory
                .select(task.count())
                .from(task)
                .where(
                        task.member.id.eq(memberId),
                        task.completed.eq(false),
                        task.parent.isNull(),
                        filterTypeEq(taskFilterType)
                )
                .fetchOne();
        return totalCount != null ? totalCount : 0;
    }

    @Override
    public Optional<Task> findByIdWithProject(Long taskId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(task)
                .leftJoin(task.project, project).fetchJoin()
                .where(task.id.eq(taskId))
                .fetchOne());
    }
}

