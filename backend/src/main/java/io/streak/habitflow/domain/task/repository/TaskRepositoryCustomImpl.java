package io.streak.habitflow.domain.task.repository;

import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTask.task;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.label.entity.QLabel;
import io.streak.habitflow.domain.task.dto.request.TaskSearchCondition;
import io.streak.habitflow.domain.task.dto.request.TaskUpdateRequest;
import io.streak.habitflow.domain.task.dto.response.TaskResponse;
import io.streak.habitflow.domain.task.entity.QTaskLabel;
import io.streak.habitflow.domain.task.entity.Task;
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
    public Optional<Task> searchTaskInfo(Long id) {
        Task result =  queryFactory
                .selectFrom(task)
                .leftJoin(task.project, project).fetchJoin()
                .where(task.id.eq(id))
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
    public List<TaskResponse> searchKeyword(String keyword, String email) {
        return
                queryFactory
                        .select(Projections.fields(
                                TaskResponse.class,
                                task.name
                        ))
                        .from(task)
                        .where(
                                task.name.contains(keyword),
                                task.member.email.eq(email)
                        )
                        .fetch();
    }

    @Override
    public List<Task> searchTasksByCondition(TaskSearchCondition taskSearchCondition, Long memberId) {
        return queryFactory
                .selectFrom(task)
                .leftJoin(task.taskLabels, QTaskLabel.taskLabel).fetchJoin()
                .leftJoin(QTaskLabel.taskLabel.label, QLabel.label).fetchJoin()
                .where(task.member.id.eq(memberId),
                        filterTypeEq(taskSearchCondition.getFilterType()))
                .orderBy(task.dueDate.desc(),task.createdAt.desc())
                .fetch();
    }

    private BooleanExpression filterTypeEq(String filterType){
        if(!StringUtils.hasText(filterType)) return null;
        LocalDateTime now = LocalDateTime.now();
        if("TODAY".equals(filterType)){
            return task.dueDate.between(now.toLocalDate().atStartOfDay(), now.toLocalDate().atTime(23,59,59));
        }else if("UPCOMING".equals(filterType)){
            return task.dueDate.goe(now.toLocalDate().plusDays(1).atStartOfDay());
        }
        return null;
    }
}
