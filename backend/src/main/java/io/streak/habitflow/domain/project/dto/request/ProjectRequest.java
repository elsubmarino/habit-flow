package io.streak.habitflow.domain.project.dto.request;

import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProjectRequest {
    public record Create(
            @NotBlank(message = "이름을 입력하세요.")
            @Size(max=100,message = "100자 이상을 초과할 수 없습니다.")
            @Schema(description = "생성할 프로젝트명 (100자이하)", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @NotBlank(message = "색상을 입력하세요")
            @Schema(description = "적용할 색상(hexacode로 입력)", requiredMode = Schema.RequiredMode.REQUIRED, example = "#123456")
            String color,

            @Schema(description = "상위 프로젝트 지정")
            Long parentId,

            @NotNull
            @Schema(description = "접근 제어자 (PUBLIC/PRIVATE)")
            AccessType accessType,

            @Schema(description = "즐겨찾기 여부")
            boolean favorite,

            @NotNull
            @Schema(description = "레이아웃 (리스트형, 보드형, 달력형)")
            LayoutType layoutType
    ){}

    public record Invite(
            @Size(max=100,message = "백명이상 초대할 수 없습니다.")
            @NotBlank(message = "초대할 이메일을 입력하세요")
            @Schema(description = "초대할 이메일 주소 리스트", requiredMode = Schema.RequiredMode.REQUIRED)
            List<String>emails
    ){
        public Invite{
            emails = Objects.requireNonNullElse(emails, new ArrayList<>());
        }
    }

    public record UpdateSortOrder(
            @Schema(description = "정렬순서")
            Long sortOrder
    ){
    }
}
