package io.streak.habitflow.domain.task.repository;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.comment.entity.QComment;
import io.streak.habitflow.domain.label.entity.QLabel;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskListQuery;
import io.streak.habitflow.domain.task.dto.response.TaskListResponse;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.QTaskInstance;
import io.streak.habitflow.domain.task.entity.QTaskLabel;
import io.streak.habitflow.domain.task.entity.QTaskMaster;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.type.TaskFilterType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTaskMaster.taskMaster;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class TaskMasterRepositoryCustomImpl implements TaskMasterRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<TaskMaster> searchTasks(TaskUpdateRequest taskUpdateRequest, String email) {
        return queryFactory
                .selectFrom(taskMaster)
                .where(
                        nameContains(taskUpdateRequest.getName()),
                        descriptionContains(taskUpdateRequest.getDescription()),
                        taskMaster.member.email.eq(email)
                )
                .orderBy(taskMaster.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<TaskMaster> searchTaskInfo(Long taskId) {
        TaskMaster result =  queryFactory
                .selectFrom(taskMaster)
                .leftJoin(taskMaster.project, project).fetchJoin()
                .where(taskMaster.id.eq(taskId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? taskMaster.name.contains(name) : null;
    }
    private BooleanExpression descriptionContains(String description) {
        return StringUtils.hasText(description) ? taskMaster.description.contains(description) : null;
    }

    @Override
    public List<TaskResponse> searchKeyword(String keyword, Long memberId, Pageable pageable) {
        return
                queryFactory
                        .select(Projections.fields(
                                TaskResponse.class,
                                taskMaster.id,
                                taskMaster.name
                        ))
                        .from(taskMaster)
                        .where(
                                taskMaster.name.contains(keyword),
                                taskMaster.member.id.eq(memberId)
                        )
                        .limit(pageable.getPageSize())
                        .fetch();
    }

    @Override
    public List<TaskListQuery> searchTasksByCondition(TaskSearchCondition taskSearchCondition, Long memberId, Pageable pageable) {
        QTaskMaster subTask = new QTaskMaster("subTask");
        QTaskInstance taskInstance = QTaskInstance.taskInstance;
        QTaskInstance subTaskInstance = new QTaskInstance("subTaskInstance");
        QComment comment = QComment.comment;

        return queryFactory
                .select(
                        Projections.fields(
                                TaskListQuery.class,
                                taskMaster.id,
                                taskMaster.name,
                                taskMaster.description,
                                taskMaster.taskPriorityType,
                                taskInstance.dueDate,
                                taskMaster.sortOrder,
                                taskMaster.project.name.as("projectName"),
                                taskInstance.id.as("taskInstanceId"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTask.count())
                                                .from(subTask)
                                                .where(subTask.parent.eq(taskMaster)),"countSubTasks"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(subTaskInstance.count())
                                                .from(subTaskInstance)
                                                .join(subTaskInstance.taskMaster, subTask)
                                                .where(subTask.parent.eq(taskMaster)
                                                        .and(subTaskInstance.isCompleted.eq(true))),"countSubTasksCompleted"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(comment.count())
                                                .from(comment)
                                                .where(comment.taskMaster.eq(taskMaster)),"countComments")
                                )
                )
                .from(taskMaster)
                .leftJoin(taskMaster.project, project)
                .join(taskMaster.taskInstances, taskInstance).on(taskInstance.isCompleted.eq(false))
                .leftJoin(taskMaster.taskLabels, QTaskLabel.taskLabel)
                .leftJoin(QTaskLabel.taskLabel.label, QLabel.label)
                .where(taskMaster.member.id.eq(memberId),
                        filterTypeEq(taskSearchCondition.getTaskFilterType(), taskInstance)
                )
                .orderBy(
                        taskInstance.dueDate.asc(),
                        taskMaster.taskPriorityType.asc(),
                        taskMaster.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize()+1)
                .fetch();
    }

    private BooleanExpression filterTypeEq(TaskFilterType taskFilterType, QTaskInstance taskInstance) {
        if(taskFilterType == null) return null;
        LocalDate localDate = LocalDate.now();
        if(TaskFilterType.TODAY == taskFilterType){
            return taskInstance.dueDate.isNotNull().and(taskInstance.dueDate.loe(localDate));
        }else if(TaskFilterType.UPCOMING == taskFilterType){
            return taskInstance.dueDate.isNotNull();
        }else if(TaskFilterType.INBOX == taskFilterType){
            return taskMaster.project.isNull();
        }
        return null;
    }

    private BooleanExpression ltTaskId(Long taskId){
        if(taskId == null) return null;
        return taskMaster.id.lt(taskId);
    }

    @Override
    public long countTasksByCondition(TaskFilterType taskFilterType, Long memberId) {
        QTaskInstance taskInstance = QTaskInstance.taskInstance;
        Long totalCount =  queryFactory
                .select(taskMaster.count())
                .from(taskMaster)
                .join(taskMaster.taskInstances, taskInstance).on(taskInstance.isCompleted.eq(false))
                .where(
                        taskMaster.member.id.eq(memberId),
                        filterTypeEq(taskFilterType, taskInstance)
                )
                .fetchOne();
        return totalCount != null ? totalCount : 0;
    }

}
