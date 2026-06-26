package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.task.entity.Task;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task,Long>, TaskRepositoryCustom {
    long countByProject(Project project);
    long countByProjectAndMember(Project project, Member member);

    default Task getOrThrow(Long taskId) {
        return findById(taskId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 테스크입니다."));
    }
}
