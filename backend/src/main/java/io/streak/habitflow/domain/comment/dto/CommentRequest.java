package io.streak.habitflow.domain.comment.dto;

import jakarta.persistence.ManyToOne;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    private Long id;
    private String content;
    private Long taskId;
}
