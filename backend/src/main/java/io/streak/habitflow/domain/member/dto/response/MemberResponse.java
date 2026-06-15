package io.streak.habitflow.domain.member.dto.response;

import io.streak.habitflow.domain.member.entity.Member;
import lombok.Builder;

public final class MemberResponse{
    @Builder
    public record Detail(
            String userId,
            String name,
            String email,
            String role
    ){
        public static Detail from(Member member) {
            return Detail.builder()
                    .name(member.getName())
                    .email(member.getEmail())
                    .role(member.getRole().name())
                    .build();
        }
    }

}
