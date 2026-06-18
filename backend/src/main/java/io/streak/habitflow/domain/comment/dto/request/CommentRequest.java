package io.streak.habitflow.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CommentRequest {
    public record Create(
            @NotBlank(message = "내용은 필수 입력 항목입니다.")
            @Size(max=2000,message = "내용은 2000자를 초과할 수 없습니다.")
            String content,
            Long taskId
    ){}
    public record Update(
            @NotNull
            Long id,
            @Size(max=2000,message = "내용은 2000자를 초과할 수 없습니다.")
            String content
    ){}
}