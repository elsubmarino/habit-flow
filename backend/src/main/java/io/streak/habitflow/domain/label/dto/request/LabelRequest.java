package io.streak.habitflow.domain.label.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class LabelRequest {
    public record Create(
            @NotBlank(message = "이름은 필수 입력 사항입니다.")
            @Size(max=100,message = "이름은 100자 이상 초과할 수 없습니다.")
            String name,

            @NotBlank(message = "색상을 선택하세요.")
            String color,
            boolean favorite
    ){}

    public record Update(
            @NotBlank
            Long id,
            @NotBlank(message = "이름은 필수 입력 사항입니다.")
            @Size(max=100,message = "이름은 100자 이상 초과할 수 없습니다.")
            String name,
            @NotBlank(message = "색상을 선택하세요.")
            String color,
            boolean favorite
    ){}
}
