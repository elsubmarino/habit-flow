package io.streak.habitflow.domain.task.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.task.dto.query.TaskSearchSummaryQuery;
import io.streak.habitflow.domain.task.dto.query.TaskSummaryQuery;
import io.streak.habitflow.domain.task.dto.request.TaskRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.QTask;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.CursorDirection;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import io.streak.habitflow.domain.task.type.TaskPriorityType;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.streak.habitflow.domain.comment.entity.QComment.comment;
import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.project.entity.QProjectMember.projectMember;
import static io.streak.habitflow.domain.task.entity.QTask.task;
import static io.streak.habitflow.domain.task.entity.QTaskLabel.taskLabel;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final HashidsProvider hashidsProvider;

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? task.name.contains(name) : null;
    }

    private BooleanExpression descriptionContains(String description) {
        return StringUtils.hasText(description) ? task.description.contains(description) : null;
    }

    @Override
    public List<TaskSearchSummaryQuery> searchKeyword(String keyword, Long memberId, Pageable pageable) {

        List<Long> ids = queryFactory
                .select(projectMember.project.id)
                .from(projectMember)
                .where(projectMember.member.id.eq(memberId))
                .fetch();

        if(ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return
                queryFactory
                        .select(Projections.constructor(
                                TaskSearchSummaryQuery.class,
                                task.id,
                                task.name,
                                task.description,
                                task.taskPriorityType,
                                task.sortOrder,
                                task.project.name
                        ))
                        .from(task)
                        .leftJoin(task.project,project)
                        .where(
                                task.project.id.in(ids),
                                task.name.contains(keyword)
                        )
                        .limit(pageable.getPageSize())
                        .fetch();
    }

    @Override
    public List<TaskSummaryQuery> findTasksByProject(Long projectId, Long memberId, Pageable pageable) {

        List<Long> projectIds = queryFactory
                .select(projectMember.project.id)
                .from(projectMember)
                .where(
                        projectMember.member.id.eq(memberId),
                        projectMember.project.id.eq(projectId))
                .fetch();

        List<Long> ids = queryFactory
                .select(task.id)
                .from(task)
                .where(task.project.id.in(projectIds),
                        task.parent.id.isNull(),
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

        return getTaskListQueries(ids);
    }

    public List<TaskSummaryQuery> getTaskListQueries(List<Long> ids) {
        if(ids == null || ids.isEmpty()){
            return Collections.emptyList();
        }

        List<Tuple> mainTasks = queryFactory
                .select(
                        task.id,
                        task.name,
                        task.description,
                        task.taskPriorityType,
                        task.dueDate,
                        task.sortOrder,
                        task.project.name,
                        task.timeSpecified
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

        QTask subTask = new QTask("subTask");
        List<Tuple> subTaskCounts = queryFactory
                .select(
                        subTask.parent.id,
                        subTask.count(),
                        new CaseBuilder()
                                .when(subTask.completed.eq(true))
                                .then(1L)
                                .otherwise(0L)
                                .sum()
                )
                .from(subTask)
                .where(
                        subTask.parent.id.in(ids)
                )
                .groupBy(
                        subTask.parent.id
                )
                .fetch();

        Map<Long, Long> subTaskTotalMap = new HashMap<>();
        Map<Long, Long> subTaskCompletedMap = new HashMap<>();
        for(Tuple row : subTaskCounts){
            Long parentId = row.get(subTask.parent.id);
            subTaskTotalMap.put(parentId, row.get(1,Long.class));

            Number completedSum = row.get(2,Number.class);
            subTaskCompletedMap.put(parentId, completedSum != null ? completedSum.longValue(): 0L );
        }

        List<Tuple> commentCounts = queryFactory
                .select(
                        comment.task.id,
                        comment.count()
                )
                .from(comment)
                .where(comment.task.id.in(ids))
                .groupBy(comment.task.id)
                .fetch();

        Map<Long, Long> commentCountMap = commentCounts.stream()
                .collect(Collectors.toMap(
                        row -> row.get(comment.task.id),
                        row -> row.get(comment.count())
                ));

        return mainTasks.stream()
                .map(row -> {
                    Long taskId = row.get(task.id);
                    return new TaskSummaryQuery(
                            taskId,
                            row.get(task.name),
                            row.get(task.description),
                            row.get(task.taskPriorityType),
                            row.get(task.dueDate),
                            row.get(task.sortOrder),
                            row.get(task.project.name),
                            // Map에서 꺼내오고 없으면 0 반환
                            subTaskTotalMap.getOrDefault(taskId, 0L),
                            subTaskCompletedMap.getOrDefault(taskId, 0L),
                            commentCountMap.getOrDefault(taskId, 0L),
                            Boolean.TRUE.equals(row.get(task.timeSpecified))
                    );
                })
                .toList();
    }

    @Override
    public List<TaskSummaryQuery> searchTasksByCondition(TaskRequest.SearchCondition searchCondition, TaskRequest.Cursor cursor, Long memberId, Pageable pageable) {

        boolean isPrev = cursor != null && cursor.direction() == CursorDirection.PREV;
        List<Long> projectIds = queryFactory
                .select(projectMember.project.id)
                .from(projectMember)
                .where(projectMember.member.id.eq(memberId))
                .fetch();

        if(projectIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> ids = queryFactory
                .select(task.id)
                .from(task)
                .where( task.project.id.in(projectIds),
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

        return getTaskListQueries(ids);
    }

    @Override
    public List<TaskSummaryQuery> searchInboxTasks(TaskRequest.SearchCondition searchCondition, TaskRequest.Cursor cursor, Long memberId, Pageable pageable) {
        boolean isPrev = cursor != null && cursor.direction() == CursorDirection.PREV;
        List<Long> ids = queryFactory
                .select(task.id)
                .from(task)
                .where( task.project.isNull(),
                        task.parent.isNull(),
                        task.completed.eq(false),
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

        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tuple> mainTasks = queryFactory
                .select(
                        task.id,
                        task.name,
                        task.description,
                        task.taskPriorityType,
                        task.dueDate,
                        task.sortOrder
                )
                .from(task)
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

        QTask subTask = new QTask("subTask");
        List<Tuple> subTaskCounts = queryFactory
                .select(
                        subTask.parent.id,
                        subTask.count(),
                        new CaseBuilder().when(subTask.completed.eq(true))
                                .then(1L)
                                .otherwise(0L)
                                .sum())
                .from(subTask)
                .where(
                        subTask.parent.id.in(ids)
                )
                .groupBy(
                        subTask.parent.id
                )
                .fetch();

        Map<Long, Long> subTaskTotalMap = new HashMap<>();
        Map<Long, Long> subTaskCompletedMap = new HashMap<>();
        for(Tuple row:subTaskCounts) {
            Long parentId = row.get(subTask.parent.id);
            subTaskTotalMap.put(parentId,row.get(1,Long.class));

            Number completedSum = row.get(2,Number.class);
            subTaskCompletedMap.put(parentId, completedSum != null ? completedSum.longValue() : 0L);
        }

        List<Tuple> commentCounts = queryFactory
                .select(
                        comment.task.id,
                        comment.count()
                )
                .from(comment)
                .where(comment.task.id.in(ids))
                .groupBy(comment.task.id)
                .fetch();

        Map<Long, Long> commentCountMap = commentCounts.stream()
                .collect(Collectors.toMap(
                        row->row.get(comment.task.id),
                        row->row.get(comment.count())
                ));

        return mainTasks.stream()
                .map(row->{
                    Long taskId = row.get(task.id);
                    return new TaskSummaryQuery(
                            taskId,
                            row.get(task.name),
                            row.get(task.description),
                            row.get(task.taskPriorityType),
                            row.get(task.dueDate),
                            row.get(task.sortOrder),
                            Expressions.constant("관리함").toString(),
                            subTaskTotalMap.getOrDefault(taskId,0L),
                            subTaskCompletedMap.getOrDefault(taskId,0L),
                            commentCountMap.getOrDefault(taskId,0L),
                            Boolean.TRUE.equals(row.get(task.timeSpecified))
                    );
                })
                .toList();
    }

    private BooleanExpression cursorCondition(TaskRequest.Cursor cursor){
        if(cursor == null || cursor.lastTaskId() == null){
            return null;
        }

        LocalDateTime lastDue = cursor.lastDueDate();
        TaskPriorityType lastPriority = cursor.lastPriorityType();
        Long lastSortOrder = cursor.lastSortOrder();
        Long lastTaskId = hashidsProvider.decode(cursor.lastTaskId());

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
        } else if (TaskFilterType.OVERDUE == taskFilterType) {
            return task.dueDate.lt(todayStart);
        }
        return null;
    }

    private BooleanExpression filterTypeEqForCount(TaskFilterType taskFilterType) {
        if (taskFilterType == null) return null;
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        if (TaskFilterType.TODAY == taskFilterType) {
            return task.dueDate.loe(todayEnd);
        } else if (TaskFilterType.INBOX == taskFilterType) {
            return task.project.isNull();
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
                .innerJoin(projectMember).on(projectMember.project.eq(project)).fetchJoin()
                .where(task.id.eq(taskId))
                .fetchOne());
    }

    @Override
    public List<TaskResponse.UpcomingDateCount> countUpcomingTasksByDate(Long memberId, LocalDateTime fromDate, LocalDateTime toDate) {
        List<Long> projectIds = queryFactory
                .select(projectMember.project.id)
                .from(projectMember)
                .where(projectMember.member.id.eq(memberId))
                .fetch();
        if(projectIds == null || projectIds.isEmpty()) return Collections.emptyList();
        return queryFactory
                .select(
                        task.dueDate.year(),
                        task.dueDate.month(),
                        task.dueDate.dayOfMonth(),
                        task.count()
                )
                .from(task)
                .where(
                        task.project.id.in(projectIds),
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
                        filterTypeEqForCount(TaskFilterType.INBOX)
                )
                .fetchOne();
        Long todayTasksCount = queryFactory
                .select(task.count())
                .from(task)
                .innerJoin(task.project,project)
                .innerJoin(projectMember).on(projectMember.project.eq(project))
                .where(
                        projectMember.member.id.eq(memberId),
                        task.completed.eq(false),
                        task.parent.isNull(),
                        filterTypeEqForCount(TaskFilterType.TODAY)
                )
                .fetchOne();
        return new TaskResponse.SidebarTasksCount(inboxTasksCount != null ? inboxTasksCount : 0L
                , todayTasksCount != null ? todayTasksCount : 0L);
    }

    @Override
    public boolean existsByIdAndHasAccess(Long taskId, Long memberId) {
        Integer fetchOne =  queryFactory
                .selectOne()
                .from(task)
                .leftJoin(task.project,project)
                .leftJoin(projectMember).on(projectMember.project.eq(project))
                .where(
                        task.id.eq(taskId),
                        (task.member.id.eq(memberId).or(projectMember.member.id.eq(memberId))
                ))
                .fetchFirst();
        return fetchOne != null;
    }

    @Override
    public List<TaskSummaryQuery> findByLabel(Long labelId, Pageable pageable, Long loginMemberId) {
        List<Long> ids =  queryFactory
                .select(task.id)
                .from(taskLabel)
                .where(taskLabel.label.id.eq(labelId))
                .fetch();
        if(ids == null || ids.isEmpty()){
            return Collections.emptyList();
        }

        List<Tuple> mainTasks = queryFactory
                .select(
                        task.id,
                        task.name,
                        task.description,
                        task.taskPriorityType,
                        task.dueDate,
                        task.sortOrder,
                        task.project.name,
                        task.timeSpecified
                )
                .from(task)
                .innerJoin(task.project,project)
                .innerJoin(projectMember).on(projectMember.project.eq(project))
                .where(
                        projectMember.member.id.eq(loginMemberId),
                        task.id.in(ids),
                        task.parent.id.isNull(),
                        task.completed.eq(false)
                )
                .orderBy(
                        task.dueDate.asc(),
                        task.taskPriorityType.asc(),
                        task.sortOrder.asc(),
                        task.id.desc())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        QTask subTask = new QTask("subTask");
        List<Tuple> subTaskCounts = queryFactory
                .select(
                        subTask.parent.id,
                        subTask.count(),
                        new CaseBuilder()
                                .when(subTask.completed.eq(true))
                                .then(1L)
                                .otherwise(0L)
                                .sum()
                )
                .from(subTask)
                .where(
                        subTask.parent.id.in(ids)
                )
                .groupBy(
                        subTask.parent.id
                )
                .fetch();

        Map<Long, Long> subTaskTotalMap = new HashMap<>();
        Map<Long, Long> subTaskCompletedMap = new HashMap<>();
        for(Tuple row : subTaskCounts){
            Long parentId = row.get(subTask.parent.id);
            subTaskTotalMap.put(parentId, row.get(1,Long.class));

            Number completedSum = row.get(2,Number.class);
            subTaskCompletedMap.put(parentId, completedSum != null ? completedSum.longValue(): 0L );
        }

        List<Tuple> commentCounts = queryFactory
                .select(
                        comment.task.id,
                        comment.count()
                )
                .from(comment)
                .where(comment.task.id.in(ids))
                .groupBy(comment.task.id)
                .fetch();

        Map<Long, Long> commentCountMap = commentCounts.stream()
                .collect(Collectors.toMap(
                        row -> row.get(comment.task.id),
                        row -> row.get(comment.count())
                ));


        return mainTasks.stream()
                .map(row -> {
                    Long taskId = row.get(task.id);
                    return new TaskSummaryQuery(
                            taskId,
                            row.get(task.name),
                            row.get(task.description),
                            row.get(task.taskPriorityType),
                            row.get(task.dueDate),
                            row.get(task.sortOrder),
                            row.get(task.project.name),
                            // Map에서 꺼내오고 없으면 0 반환
                            subTaskTotalMap.getOrDefault(taskId, 0L),
                            subTaskCompletedMap.getOrDefault(taskId, 0L),
                            commentCountMap.getOrDefault(taskId, 0L),
                            Boolean.TRUE.equals(row.get(task.timeSpecified))
                    );
                })
                .toList();
    }
}

