package io.streak.habitflow.domain.member.dto.response;

import io.streak.habitflow.domain.member.entity.Member;
import lombok.Builder;

@Builder
public record MemberResponse(
        String userId,
        String name,
        String email,
        String role
) {
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole().name())
                .build();
    };
}
