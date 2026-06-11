package io.streak.habitflow.domain.member.dto.response;

import io.streak.habitflow.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
public record MemberResponse(
        Long id,
        String userId,
        String name,
        String email,
        String role
) {
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .name(member.getName())
                .email(member.getEmail())
                .build();
    };
}
