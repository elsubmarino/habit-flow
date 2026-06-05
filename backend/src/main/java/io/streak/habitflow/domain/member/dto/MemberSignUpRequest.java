package io.streak.habitflow.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberSignUpRequest {
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message="이름은 필수입니다.")
    private String name;

    @NotBlank(message="이메일은 필수입니다.")
    private String email;
}
