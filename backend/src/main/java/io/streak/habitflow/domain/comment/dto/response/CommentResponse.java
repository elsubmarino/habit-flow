package io.streak.habitflow.domain.comment.dto.response;

import io.streak.habitflow.domain.attachment.dto.response.AttachmentResponse;
import io.streak.habitflow.domain.comment.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private String content;

    @Builder.Default
    private List<AttachmentResponse> attachments = new ArrayList<>();

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .content(comment.getContent())
                .attachments(comment.getAttachments().stream()
                        .map(AttachmentResponse::from)
                        .toList())
                .build();
    }
}
