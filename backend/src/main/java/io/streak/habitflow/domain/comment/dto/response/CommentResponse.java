package io.streak.habitflow.domain.comment.dto.response;

import io.streak.habitflow.domain.attachment.dto.response.AttachmentResponse;
import io.streak.habitflow.domain.comment.entity.Comment;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommentResponse{
    @Builder
    public record Detail(
            String content,
            List<AttachmentResponse.Detail> attachments
    ) {
        public Detail{
            attachments = Objects.requireNonNullElse(attachments, new ArrayList<>());
        }

        public static Detail from(Comment comment) {
            return Detail.builder()
                    .content(comment.getContent())
                    .attachments(comment.getAttachments().stream()
                            .map(AttachmentResponse.Detail::from)
                            .toList())
                    .build();
        }
    }
}

