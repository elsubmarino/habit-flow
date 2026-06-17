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
import io.streak.habitflow.domain.task.type.CursorDirection;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.streak.habitflow.domain.comment.entity.QComment.comment;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTask.task;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {
    private final JPAQueryFactory queryFactory;

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
                                task.timeSpecified,
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
    public List<TaskListQuery> searchTasksByCondition(TaskRequest.SearchCondition searchCondition, TaskRequest.Cursor cursor, Long memberId, Pageable pageable) {

        boolean isPrev = cursor != null && cursor.direction() == CursorDirection.PREV;
        List<Long> ids = queryFactory
                .select(task.id)
                .from(task)
                .where(task.member.id.eq(memberId),
                        task.parent.isNull(),
                        task.completed.eq(false),
                        filterTypeEq(searchCondition.taskFilterType()),
                        dateRangeEq(searchCondition.fromDate(),searchCondition.toDate()),
                        cursorCondition(cursor)
                )
                .orderBy(
                        isPrev?task.dueDate.desc():task.dueDate.asc(),
                        isPrev?task.taskPriorityType.desc():task.taskPriorityType.asc(),
                        isPrev?task.sortOrder.desc():task.sortOrder.asc(),
                        isPrev?task.id.asc():task.id.desc()
                )
                .limit(pageable.getPageSize() + 1)
                .fetch();

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }


        QTask subTask = new QTask("subTask");

        List<TaskListQuery> rows = queryFactory
                .select(
                        Projections.fields(
                                TaskListQuery.class,
                                task.id,
                                task.name,
                                task.description,
                                task.taskPriorityType,
                                task.dueDate,
                                task.timeSpecified,
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
        if(isPrev){
            Collections.reverse(rows);
        }
        return rows;
    }

    private BooleanExpression cursorCondition(TaskRequest.Cursor cursor){
        if(cursor == null || cursor.lastTaskId() == null){
            return null;
        }

        LocalDateTime lastDue = cursor.lastDueDate();
        TaskPriorityType lastPriority = cursor.lastPriorityType();
        Long lastSortOrder = cursor.lastSortOrder();
        Long lastTaskId = cursor.lastTaskId();

        if (cursor.direction() == CursorDirection.PREV) {
            // asc 정렬 기준으로 "커서 이전" 행
            BooleanExpression before = task.dueDate.lt(lastDue);
            if (lastDue != null) {
                before = before.or(
                        task.dueDate.eq(lastDue)
                                .and(lastPriority != null
                                        ? task.taskPriorityType.lt(lastPriority)
                                        : task.taskPriorityType.isNotNull())
                );
            }
            if (lastDue != null && lastPriority != null) {
                before = before.or(
                        task.dueDate.eq(lastDue)
                                .and(task.taskPriorityType.eq(lastPriority))
                                .and(lastSortOrder != null
                                        ? task.sortOrder.lt(lastSortOrder)
                                        : task.sortOrder.isNotNull())
                );
            }
            if (lastDue != null && lastPriority != null && lastSortOrder != null) {
                before = before.or(
                        task.dueDate.eq(lastDue)
                                .and(task.taskPriorityType.eq(lastPriority))
                                .and(task.sortOrder.eq(lastSortOrder))
                                .and(task.id.gt(lastTaskId))
                );
            }
            return before;
        }
        // NEXT: asc 정렬 기준으로 "커서 이후" 행
        BooleanExpression after = lastDue != null ? task.dueDate.gt(lastDue) : null;
        if (lastDue != null) {
            after = combineOr(after,
                    task.dueDate.eq(lastDue)
                            .and(lastPriority != null
                                    ? task.taskPriorityType.gt(lastPriority)
                                    : null)
            );
        }
        if (lastDue != null && lastPriority != null) {
            after = combineOr(after,
                    task.dueDate.eq(lastDue)
                            .and(task.taskPriorityType.eq(lastPriority))
                            .and(lastSortOrder != null
                                    ? task.sortOrder.gt(lastSortOrder)
                                    : null)
            );
        }
        if (lastDue != null && lastPriority != null && lastSortOrder != null) {
            after = combineOr(after,
                    task.dueDate.eq(lastDue)
                            .and(task.taskPriorityType.eq(lastPriority))
                            .and(task.sortOrder.eq(lastSortOrder))
                            .and(task.id.lt(lastTaskId))
            );
        }
        return after;
    }

    private BooleanExpression combineOr(BooleanExpression base, BooleanExpression next) {
        if (next == null) return base;
        return base == null ? next : base.or(next);
    }

    private BooleanExpression filterTypeEq(TaskFilterType taskFilterType) {
        if (taskFilterType == null) return null;
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        if (TaskFilterType.TODAY == taskFilterType) {
            return task.dueDate.goe(todayStart)
                    .and(task.dueDate.loe(todayEnd));
        } else if (TaskFilterType.UPCOMING == taskFilterType) {
            return task.dueDate.goe(todayStart);
        } else if (TaskFilterType.INBOX == taskFilterType) {
            return task.project.isNull();
        } else if (TaskFilterType.OVERDUE == taskFilterType) {
            return task.dueDate.lt(todayStart);
        }
        return null;
    }

    private BooleanExpression ltTaskId(Long taskId) {
        if (taskId == null) return null;
        return task.id.lt(taskId);
    }

    private BooleanExpression dateRangeEq(LocalDateTime fromDate, LocalDateTime toDate){
        BooleanExpression expression = null;
        if(fromDate != null){
            expression = task.dueDate.goe(fromDate);
        }
        if(toDate != null){
            BooleanExpression toExpr = task.dueDate.loe(toDate);
            expression = expression == null ? toExpr : expression.and(toExpr);
        }
        return expression;
    }

    @Override
    public Optional<Task> findByIdWithProject(Long taskId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(task)
                .leftJoin(task.project, project).fetchJoin()
                .where(task.id.eq(taskId))
                .fetchOne());
    }

    @Override
    public List<TaskResponse.UpcomingDateCount> countUpcomingTasksByDate(Long memberId, LocalDateTime fromDate, LocalDateTime toDate) {
        return queryFactory
                .select(
                        task.dueDate.year(),
                        task.dueDate.month(),
                        task.dueDate.dayOfMonth(),
                        task.count()
                )
                .from(task)
                .where(
                        task.member.id.eq(memberId),
                        task.parent.isNull(),
                        task.completed.eq(false),
                        filterTypeEq(TaskFilterType.UPCOMING),
                        dateRangeEq(fromDate, toDate)
                )
                .groupBy(
                        task.dueDate.year(),
                        task.dueDate.month(),
                        task.dueDate.dayOfMonth()
                )
                .fetch()
                .stream()
                .map(row -> {
                        Number yearNum = row.get(0, Number.class);
                        Number monthNum = row.get(1, Number.class);
                        Number dayNum = row.get(2, Number.class);
                        Long count = row.get(3, Long.class);

                        int year = (yearNum != null) ? yearNum.intValue() : 0;
                        int month = (monthNum != null) ? monthNum.intValue() : 1;
                        int day = (dayNum != null) ? dayNum.intValue() : 1;

                        return TaskResponse.UpcomingDateCount.of(
                                LocalDate.of(year, month, day),
                                (count != null) ? count : 0L
                        );
                    }
                )
                .toList();
    }

    @Override
    public TaskResponse.SidebarTasksCount countSidebarTasks(Long memberId) {
        Long inboxTasksCount = queryFactory
                .select(task.count())
                .from(task)
                .where(
                        task.member.id.eq(memberId),
                        task.completed.eq(false),
                        task.parent.isNull(),
                        filterTypeEq(TaskFilterType.INBOX)
                )
                .fetchOne();
        Long todayTasksCount = queryFactory
                .select(task.count())
                .from(task)
                .where(
                        task.member.id.eq(memberId),
                        task.completed.eq(false),
                        task.parent.isNull(),
                        filterTypeEq(TaskFilterType.TODAY)
                )
                .fetchOne();
        return new TaskResponse.SidebarTasksCount(inboxTasksCount != null ? inboxTasksCount : 0L
                , inboxTasksCount != null ? inboxTasksCount : 0L);
    }
}

