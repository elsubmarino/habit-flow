package io.streak.habitflow.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public final class MemberRequest {
    public record Login(
            @NotBlank(message = "이메일을 입력하세요.")
            @Email
            @Pattern(
                    regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
                    message = "올바른 이메일 형식이 아닙니다."
            )
            @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
            @Schema(description = "로그인할 이메일 주소", requiredMode = Schema.RequiredMode.REQUIRED, example = "asdf@asdf.com")
            String email,

            @Size(max = 100, message = "패스워드는 100자를 초과할 수 없습니다.")
            @NotBlank(message = "패스워드를 입력하세요.")
            @Schema(description = "로그인 시 필요한 패스워드", requiredMode = Schema.RequiredMode.REQUIRED)
            String password
    ) {
    }

    public record SignUp(
            @NotBlank(message = "비밀번호는 필수입니다.")
            @Schema(description = "회원가입 시 필요한 패스워드", requiredMode = Schema.RequiredMode.REQUIRED)
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                    message = "패스워드는 영문 대소문자, 숫자, 특수문자를 포함하여 8자 이상 20자 이하로 입력해야 합니다."
            )
            String password,

            @NotBlank(message = "이름은 필수입니다.")
            @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
            @Schema(description = "회원가입 시 필요한 이름 (100자 이하)", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @NotBlank(message = "이메일은 필수입니다.")
            @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
            @Schema(description = "회원가입 시 필요한 이메일", requiredMode = Schema.RequiredMode.REQUIRED, examples = {"asdf@asdf.com"})
            String email
    ) {
    }

    public record Update(
            @NotBlank(message = "변경할 이름은 필수입니다.")
            @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
            @Schema(description = "변경할 이름 (100자 이하)", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @NotBlank(message = "변경할 비밀번호는 필수입니다.")
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                    message = "패스워드는 영문 대소문자, 숫자, 특수문자를 포함하여 8자 이상 20자 이하로 입력해야 합니다."
            )
            @Schema(description = "변경할 비밀번호 (영문대소문자,숫자,특수문자를 포함하여 8자 이상 20자 이하", requiredMode = Schema.RequiredMode.REQUIRED)
            String password,

            @NotNull
            Long id
    ) {
    }

    public record SendAuthCode(
            @NotBlank @Email String email
    ) {
    }

    public record VerifyAuthCode(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String code
    ){}
}
