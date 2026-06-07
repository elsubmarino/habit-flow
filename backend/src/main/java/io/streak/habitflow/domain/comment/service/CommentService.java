package io.streak.habitflow.domain.comment.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.dto.request.CommentRequest;
import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.task.entity.Task;
import io.streak.habitflow.domain.task.repository.TaskRepository;
import io.streak.habitflow.global.infra.file.FileDto;
import io.streak.habitflow.global.infra.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Transactional
    public CommentResponse createComment(CommentRequest commentRequest, MultipartFile file, UserDetails userDetails) {
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()->new IllegalArgumentException("사용자가 없습니다."));

        Task task = taskRepository.findById(commentRequest.getTaskId())
                .orElseThrow(()->new IllegalArgumentException("테스크가 없습니다."));

        Comment comment = Comment.builder()
                .member(member)
                .task(task)
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

    public List<CommentResponse> getComments(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("테스크가 없습니다."));
        List<Comment> comments = commentRepository.findByTaskId(task.getId());
        return comments.stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse updateComment(Long id, CommentRequest request, UserDetails userDetails) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 댓글입니다."));
        if(!comment.getMember().getEmail().equals(userDetails.getUsername())){
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        comment.updateContent(request.getContent());
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long id){
        commentRepository.deleteById(id);
    }
}
