package io.streak.habitflow.domain.task.repository;

import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTask.task;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.comment.entity.QComment;
import io.streak.habitflow.domain.label.entity.QLabel;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.QTask;
import io.streak.habitflow.domain.task.entity.QTaskLabel;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Task> searchTasks(TaskUpdateRequest taskUpdateRequest, String email) {
        return queryFactory
                .selectFrom(task)
                .where(
                        nameContains(taskUpdateRequest.getName()),
                        descriptionContains(taskUpdateRequest.getDescription()),
                        task.member.email.eq(email)
                )
                .orderBy(task.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<Task> searchTaskInfo(Long taskId) {
        Task result =  queryFactory
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
    public List<TaskResponse> searchKeyword(String keyword, Long memberId) {
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
                        .limit(5)
                        .fetch();
    }

    @Override
    public List<TaskListResponse> searchTasksByCondition(TaskSearchCondition taskSearchCondition, Long memberId) {
        QTask subTask = new QTask("subTask");
        QComment comment = QComment.comment;
        int pageSize = 20;

        return queryFactory
                .select(
                        Projections.fields(
                                TaskListResponse.class,
                                task.id,
                                task.name,
                                task.description,
                                task.taskPriorityType,
                                task.dueDate,
                                task.sortOrder,
                                task.project.name.as("projectName"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTask.count())
                                                .from(subTask)
                                                .where(subTask.parent.eq(task)),"countSubTasks"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTask.count())
                                                .from(subTask)
                                                .where(subTask.parent.eq(task),
                                                        subTask.completed),"countSubTasksCompleted"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(comment.count())
                                                .from(comment)
                                                .where(comment.task.eq(task)),"countComments")
                                )
                )
                .from(task)
                .leftJoin(task.project, project)
                .leftJoin(task.taskLabels, QTaskLabel.taskLabel)
                .leftJoin(QTaskLabel.taskLabel.label, QLabel.label)
                .where(task.member.id.eq(memberId),
                        filterTypeEq(taskSearchCondition.getTaskFilterType()),
                        ltTaskId(taskSearchCondition.getLastTaskId())
                )
                .orderBy(task.id.desc())
                .limit(pageSize+1)
                .fetch();
    }

    private BooleanExpression filterTypeEq(TaskFilterType taskFilterType){
        if(taskFilterType == null) return null;
        LocalDateTime now = LocalDateTime.now();
        if(TaskFilterType.TODAY == taskFilterType){
            return task.dueDate.loe(now.toLocalDate().atTime(23,59,59));
        }else if(TaskFilterType.INBOX == taskFilterType){
            return task.project.isNull();
        }
        return null;
    }

    private BooleanExpression ltTaskId(Long taskId){
        if(taskId == null) return null;
        return task.id.lt(taskId);
    }

    @Override
    public long countTasksByCondition(TaskFilterType taskFilterType, Long memberId) {
        Long totalCount =  queryFactory
                .select(task.count())
                .from(task)
                .where(
                        task.member.id.eq(memberId),
                        filterTypeEq(taskFilterType)
                )
                .fetchOne();
        return totalCount != null ? totalCount : 0;
    }

}
