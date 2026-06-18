package io.streak.habitflow.domain.label.dto.request;


import io.streak.habitflow.global.validator.HexColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class LabelRequest {
    public record Create(
            @NotBlank(message = "이름은 필수 입력 사항입니다.")
            @Size(max=100,message = "이름은 100자 이상 초과할 수 없습니다.")
            @Schema(description = "라벨 이름(100자 이하)", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @NotBlank(message = "색상을 선택하세요.")
            @HexColor(message = "잘못된 색상입니다.")
            @Schema(description = "라벨 색상 (헥사코드)", requiredMode = Schema.RequiredMode.REQUIRED, examples ={"#123456"})
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
            @HexColor(message = "잘못된 색상입니다.")
            String color,
            boolean favorite
    ){}
}
