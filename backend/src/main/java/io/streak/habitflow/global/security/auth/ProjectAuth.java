package io.streak.habitflow.global.security.auth;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("projectAuth")
@RequiredArgsConstructor
public class ProjectAuth {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    public boolean canAccess(UUID publicProjectId){
        Project project = projectRepository.getOrThrowByProjectId(publicProjectId);
        Member member = memberRepository.getReferenceById(SecurityUtils.currentMemberId());
        return projectMemberRepository.existsByProjectAndMember(project,member);
    }
}
