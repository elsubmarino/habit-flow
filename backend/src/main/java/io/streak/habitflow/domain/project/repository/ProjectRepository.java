package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.project.entity.Project;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom {
    @Query("SELECT pu.project FROM ProjectMember pu WHERE pu.member.email = :email")
    List<Project> findByMemberEmail(@Param("email") String email);

    List<Project> findByNameContaining(String name);

    default Project getOrThrow(Long projectId){
        return findById(projectId).orElseThrow(()->new EntityNotFoundException("프로젝트가 존재하지 않습니다."));
    }
}
