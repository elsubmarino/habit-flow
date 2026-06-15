package io.streak.habitflow.domain.comment.dto.request;

public final class CommentRequest {
    public record Create(
            String content,
            Long taskId
    ){}
    public record Update(
            Long id,
            String content,
            Long taskId
    ){}
}
