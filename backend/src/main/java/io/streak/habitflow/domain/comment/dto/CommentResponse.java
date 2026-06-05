package io.streak.habitflow.domain.comment.dto;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class CommentResponse {
    private String content;
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .content(comment.getContent())
                .attachments(comment.getAttachments())
                .build();
    }
}
