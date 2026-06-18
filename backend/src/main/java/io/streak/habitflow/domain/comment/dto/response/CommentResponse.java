package io.streak.habitflow.domain.comment.dto.response;

import io.streak.habitflow.domain.attachment.dto.response.AttachmentResponse;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommentResponse{
    @Builder
    public record Detail(
            @Schema(description = "댓글 내용 (최대 2000자)")
            String content,

            @Schema(description = "첨부파일")
            List<AttachmentResponse.Detail> attachments,
            Long id
    ) {
        public Detail{
            attachments = Objects.requireNonNullElse(attachments, new ArrayList<>());
        }

        public static Detail from(Comment comment) {
            return Detail.builder()
                    .content(comment.getContent())
                    .id(comment.getId())
                    .attachments(comment.getAttachments().stream()
                            .map(AttachmentResponse.Detail::from)
                            .toList())
                    .build();
        }
    }
}

