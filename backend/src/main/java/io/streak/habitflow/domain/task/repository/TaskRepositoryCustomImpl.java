package io.streak.habitflow.domain.task.repository;

import static io.streak.habitflow.domain.project.entity.QProject.project;
import static io.streak.habitflow.domain.task.entity.QTask.task;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.streak.habitflow.domain.task.dto.TaskRequest;
import io.streak.habitflow.domain.task.dto.TaskResponse;
import io.streak.habitflow.domain.task.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Task> searchTasks(TaskRequest taskRequest) {
        return queryFactory
                .selectFrom(task)
                .where(
                        titleContains(taskRequest.getTitle()),
                        descriptionContains(taskRequest.getDescription())
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

    private BooleanExpression titleContains(String title) {
        return StringUtils.hasText(title) ? task.title.contains(title) : null;
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
                                task.title
                        ))
                        .from(task)
                        .where(
                                task.title.contains(keyword),
                                task.member.email.eq(email)
                        )
                        .fetch();
    }
}
