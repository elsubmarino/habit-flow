package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom {
    @Query("SELECT pu.project FROM ProjectMember pu WHERE pu.member.email = :email")
    List<Project> findByMemberEmail(@Param("email") String email);

    List<Project> findByNameContaining(String name);
    Optional<Project> findByPublicId(UUID publicId);

    List<Project> findAllByPublicIdIn(Collection<UUID> publicId);

    default Project getOrThrow(Long projectId){
        return findById(projectId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }

    default Project getOrThrowByPublicId(UUID publicId){
        return findByPublicId(publicId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
}
