package io.streak.habitflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserUpdateRequest {
    @NotBlank(message = "변경할 이름은 필수입니다.")
    private String userName;

    private String email;
}
