package io.streak.habitflow.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberUpdateRequest {
    @NotBlank(message = "변경할 이름은 필수입니다.")
    private String userName;

    private String email;
}
