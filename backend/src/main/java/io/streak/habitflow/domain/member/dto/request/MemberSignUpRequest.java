package io.streak.habitflow.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSignUpRequest {
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message="이름은 필수입니다.")
    private String name;

    @NotBlank(message="이메일은 필수입니다.")
    private String email;
}
