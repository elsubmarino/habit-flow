package io.streak.habitflow.domain.comment.dto;

import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentRequest {
    private Long id;
    private String content;
    private Long taskId;
}
