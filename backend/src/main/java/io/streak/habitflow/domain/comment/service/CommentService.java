package io.streak.habitflow.domain.comment.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.dto.request.CommentRequest;
import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.domain.task.type.TargetType;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Transactional
    public CommentResponse.Detail createComment(CommentRequest.Create request, MultipartFile file, Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);

        Task task = taskRepository.getOrThrow(request.taskId());

        Comment comment = Comment.builder()
                .member(member)
                .task(task)
                .content(request.content())
                .build();

        if(file != null && !file.isEmpty()){
            FileDto fileDto = fileStorageService.upload(file);

            Attachment attachment = Attachment.builder()
                    .originalFileName(fileDto.originalFileName())
                    .savedFileName((fileDto.savedFileName()))
                    .fileUrl(fileDto.fileUrl())
                    .build();
            comment.addAttachment(attachment);
        }

        Comment result = commentRepository.save(comment);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.COMMENT,
                ActivityType.ADDED,
                "당신이 "+task.getName()+"에 댓글을 추가했습니다"
        ));

        return CommentResponse.Detail.from(result);
    }

    public List<CommentResponse.Detail> getComments(Long taskId) {
        Task task = taskRepository.getOrThrow(taskId);
        List<Comment> comments = commentRepository.findByTaskIdWithAttachments(task.getId());
        return comments.stream()
                .map(CommentResponse.Detail::from)
                .toList();
    }

    @Transactional
    @CheckOwnership(type="COMMENT")
    @SuppressWarnings("unused")
    public void updateComment(Long commentId, CommentRequest.Update request, Long memberId) {
        Comment comment = commentRepository.getOrThrow(commentId);

        comment.updateContent(request.content());
    }

    @Transactional
    @CheckOwnership(type="COMMENT")
    @SuppressWarnings("unused")
    public void deleteComment(Long commentId, Long memberId){
        Comment comment  =commentRepository.getReferenceById(commentId);
        commentRepository.deleteById(commentId);
        Task task = comment.getTask();
        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                task.getId(),
                memberId,
                TargetType.COMMENT,
                ActivityType.ADDED,
                "당신이 "+task.getName()+"에서 댓글을 삭제했습니다"
        ));
    }

}
