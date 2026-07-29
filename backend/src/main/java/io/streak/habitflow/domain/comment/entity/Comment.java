package io.streak.habitflow.domain.comment.entity;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name="comments",indexes = {@Index(name="idx_comments_task_id",columnList = "task_id")})
public class Comment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="comment_id")
    private Long id;

    @Builder.Default
    @Column(nullable = false,unique = true,updatable = false)
    private UUID publicId = UUID.randomUUID();

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id")
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
