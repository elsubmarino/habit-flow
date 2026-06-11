package io.streak.habitflow.domain.comment.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.dto.request.CommentRequest;
import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.entity.TaskMaster;
import io.streak.habitflow.domain.task.repository.TaskMasterMasterRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
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
    private final TaskMasterMasterRepository taskMasterRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public CommentResponse createComment(CommentRequest commentRequest, MultipartFile file, Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);

        TaskMaster taskMaster = taskMasterRepository.findById(commentRequest.getTaskId())
                .orElseThrow(()->new IllegalArgumentException("테스크가 없습니다."));

        Comment comment = Comment.builder()
                .member(member)
                .taskMaster(taskMaster)
                .content(commentRequest.getContent())
                .build();

        if(file != null && !file.isEmpty()){
            FileDto fileDto = fileStorageService.upload(file);

            Attachment attachment = Attachment.builder()
                    .originalFileName(fileDto.getOriginalFileName())
                    .savedFileName((fileDto.getSavedFileName()))
                    .fileUrl(fileDto.getFileUrl())
                    .build();
            comment.addAttachment(attachment);
        }

        Comment result = commentRepository.save(comment);
        return CommentResponse.from(result);
    }

    public List<CommentResponse> getComments(Long taskId) {
        TaskMaster taskMaster = taskMasterRepository.findById(taskId)
                .orElseThrow(()->new IllegalArgumentException("테스크가 없습니다."));
        List<Comment> comments = commentRepository.findByTaskMasterId(taskMaster.getId());
        return comments.stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    @CheckOwnership(type="COMMENT")
    @SuppressWarnings("unused")
    public CommentResponse updateComment(Long commentId, CommentRequest request, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow();

        comment.updateContent(request.getContent());
        return CommentResponse.from(comment);
    }

    @Transactional
    @CheckOwnership(type="COMMENT")
    @SuppressWarnings("unused")
    public void deleteComment(Long commentId, Long memberId){
        commentRepository.deleteById(commentId);
    }

}
