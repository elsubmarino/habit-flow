package io.streak.habitflow.domain.attachment.entity;


import io.streak.habitflow.global.common.BaseTimeEntity;
import io.streak.habitflow.domain.comment.entity.Comment;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class Attachment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="comment_id")
    private Comment comment;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String savedFileName;

    private String contentType;
    private Long fileSize;

    public void changeComment(Comment comment) {
        this.comment = comment;
    }
}
