package io.streak.habitflow.domain.project.repository;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    void deleteByProjectId(Long projectId);
    boolean existsByProjectAndMember(Project project, Member member);
    List<ProjectMember> findByProject(Project project);
    long countByMember(Member member);
    void deleteByProjectAndMember(Project project, Member member);
}
