package io.streak.habitflow.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class MemberRequest {
    public record Login(
            @NotBlank(message = "이메일을 입력하세요.")
            String email,
            @NotBlank(message = "패스워드를 입력하세요.")
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
            @NotBlank(message = "변경할 비밀번호는 필수입니다.")
            String password,
            @NotNull
            Long id
    ){}
}
