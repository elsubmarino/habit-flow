package io.streak.habitflow.domain.comment.service;

import io.streak.habitflow.domain.activitylog.event.ActivityRecordedEvent;
import io.streak.habitflow.domain.comment.dto.request.CommentRequest;
import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.entity.Attachment;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.common.type.ActivityType;
import io.streak.habitflow.global.common.type.TargetType;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HashidsProvider hashidsProvider;

    @Transactional
    @CheckOwnership(type="TASK")
    public CommentResponse.Detail createComment(Long taskId, CommentRequest.Create request, FileDto fileDto, Long loginMemberId) {
        Long realTaskId = hashidsProvider.decode(request.taskId());
        Member member = memberRepository.getReferenceById(loginMemberId);

        Task task = taskRepository.getOrThrow(realTaskId);

        Comment comment = Comment.builder()
                .member(member)
                .task(task)
                .content(request.content())
                .build();

        if(fileDto != null){
            Attachment attachment = Attachment.builder()
                    .originalFileName(fileDto.originalFileName())
                    .savedFileName((fileDto.savedFileName()))
                    .fileUrl(fileDto.fileUrl())
                    .build();
            comment.addAttachment(attachment);
        }

        Comment result = commentRepository.save(comment);

        applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                task.getId(),
                loginMemberId,
                TargetType.COMMENT,
                ActivityType.ADDED,
                task.getName(),
                Collections.emptyList()
        ));

        String encodedId = hashidsProvider.encode(result.getId());
        return CommentResponse.Detail.of(result,encodedId);
    }

    @CheckOwnership(type="TASK")
    public List<CommentResponse.Detail> getComments(Long taskId, Long loginMemberId) {
        Task task = taskRepository.getOrThrow(taskId);
        List<Comment> comments = commentRepository.findByTaskIdWithAttachments(task.getId());
        return comments.stream()
                .map(comment->{
                    String encodedId = hashidsProvider.encode(comment.getId());
                    return CommentResponse.Detail.of(comment,encodedId);
                })
                .toList();
    }

    @Transactional
    @CheckOwnership(type="COMMENT")
    @SuppressWarnings("unused")
    public CommentResponse.Detail updateComment(Long commentId, CommentRequest.Update request, Long loginMemberId) {
        Comment comment = commentRepository.getOrThrow(commentId);
        String encodedId = hashidsProvider.encode(comment.getId());
        if(comment.getContent().equals(request.content())){
            return CommentResponse.Detail.of(comment,encodedId);
        }
        comment.updateContent(request.content());
        return CommentResponse.Detail.of(comment,encodedId);
    }

    @Transactional
    @CheckOwnership(type="COMMENT")
    @SuppressWarnings("unused")
    public void deleteComment(Long commentId, Long loginMemberId){
        Comment comment  =commentRepository.getReferenceById(commentId);
        commentRepository.deleteById(commentId);
        Task task = comment.getTask();
        applicationEventPublisher.publishEvent(new ActivityRecordedEvent(
                task.getId(),
                loginMemberId,
                TargetType.COMMENT,
                ActivityType.DELETED,
                task.getName(),
                Collections.emptyList()
        ));
    }

}
