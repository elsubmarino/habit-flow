package io.streak.habitflow.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public final class MemberRequest {
    public record Login(
            String email,
            String password
    ){}

    public record SignUp(
            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,

            @NotBlank(message="이름은 필수입니다.")
            String name,

            @NotBlank(message="이메일은 필수입니다.")
            String email
    ){}

    public record Update(
            @NotBlank(message = "변경할 이름은 필수입니다.")
            String name,
            String email,
            String password,
            Long id
    ){}
}
