package io.streak.habitflow.domain.member.dto.response;

import io.streak.habitflow.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public final class MemberResponse{
    @Builder
    public record Detail(
            @Schema(description = "멤버 ID",example = "1")
            Long id,

            @Schema(description = "사용자명",example = "홍길동")
            String name,

            @Schema(description = "이메일 주소",example = "asdf@adf.com")
            String email,

            @Schema(description = "역할명",examples = {"USER","ADMIN"})
            String role
    ){
        public static Detail from(Member member) {
            return Detail.builder()
                    .id(member.getId())
                    .name(member.getName())
                    .email(member.getEmail())
                    .role(member.getRole().name())
                    .build();
        }
    }

}
