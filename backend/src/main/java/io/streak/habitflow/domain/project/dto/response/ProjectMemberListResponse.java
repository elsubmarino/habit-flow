package io.streak.habitflow.domain.project.dto.response;


import io.streak.habitflow.domain.project.entity.ProjectMember;
import lombok.Builder;

@Builder
public record ProjectMemberListResponse(
        String memberName,
        String email
){
    public static ProjectMemberListResponse from(ProjectMember projectMember){
        return ProjectMemberListResponse.builder()
                .memberName(projectMember.getMember().getName())
                .email(projectMember.getMember().getEmail())
                .build();
    }
}
