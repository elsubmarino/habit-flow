package io.streak.habitflow.domain.comment.entity;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name="comments")
public class Comment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="comment_id")
    private Long id;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id",foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
        attachment.changeComment(this);
    }

    public void updateContent(String content){
        this.content = content;
    }

    public void assignTask(Task task){
        this.task = task;
    }

}
