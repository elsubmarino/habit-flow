package io.streak.habitflow.domain.project.dto.request;

import io.streak.habitflow.domain.project.type.AccessType;
import io.streak.habitflow.domain.project.type.LayoutType;
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
            String name,
            @NotBlank(message = "색상을 입력하세요")
            String color,
            Long parentId,
            @NotNull
            AccessType accessType,
            boolean favorite,
            @NotNull
            LayoutType layoutType
    ){}

    public record Invite(
            Long id,
            @Size(max=100,message = "백명이상 초대할 수 없습니다.")
            @NotBlank(message = "초대할 이메일을 입력하세요")
            List<String>emails
    ){
        public Invite{
            emails = Objects.requireNonNullElse(emails, new ArrayList<>());
        }
    }
}
