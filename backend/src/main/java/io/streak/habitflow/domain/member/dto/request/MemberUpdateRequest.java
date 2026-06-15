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
public class MemberUpdateRequest {
    @NotBlank(message = "변경할 이름은 필수입니다.")
    private String name;
    private String email;
    private String password;
    private Long id;
}
