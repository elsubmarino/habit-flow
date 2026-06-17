package io.streak.habitflow.domain.attachment.entity;


import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name="attachments")
public class Attachment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="comment_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
