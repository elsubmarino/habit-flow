package io.streak.habitflow.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CommentRequest {
    public record Create(
            @NotBlank(message = "내용은 필수 입력 항목입니다.")
            @Size(max=2000,message = "내용은 2000자를 초과할 수 없습니다.")
            @Schema(description = "댓글 내용 (최대 2000자)", requiredMode = Schema.RequiredMode.REQUIRED)
            String content,

            @NotNull
            @Schema(description = "댓글이 속한 TASK ID", requiredMode = Schema.RequiredMode.REQUIRED)
            Long taskId
    ){}
    public record Update(
            @NotNull
            @Schema(description = "수정할 댓글 ID", requiredMode = Schema.RequiredMode.REQUIRED)
            Long id,

            @Schema(description = "수정할 댓글 내용", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "내용은 필수 입력 항목입니다.")
            @Size(max=2000,message = "내용은 2000자를 초과할 수 없습니다.")
            String content
    ){}
}