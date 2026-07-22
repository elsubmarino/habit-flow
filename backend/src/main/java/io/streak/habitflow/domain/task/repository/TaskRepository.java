package io.streak.habitflow.domain.task.repository;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task,Long>, TaskRepositoryCustom {
    long countByProject(Project project);
    long countByProjectAndMember(Project project, Member member);
    long countByProjectIsNullAndMember(Member member);
    Optional<Task> findByPublicId(UUID publicTaskId);
    boolean existsByPublicIdAndHasAccess(UUID publicTaskId, Long memberId);

    default Task getOrThrow(Long taskId) {
        return findById(taskId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }

    default Task getOrThrowByPublicId(UUID publicId){
        return findByPublicId(publicId).orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
