package io.streak.habitflow.domain.comment.service;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.streak.habitflow.domain.comment.dto.CommentRequest;
import io.streak.habitflow.domain.comment.dto.CommentResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;

    /**
     * 코멘트 생성
     * @param commentRequest
     * @param file
     * @param userDetails
     * @return
     */
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

    /**
     * 코멘트 다건 조회
     * @param commentRequest
     * @return
     */
    public List<CommentResponse> getComments(CommentRequest commentRequest) {
        Task task = taskRepository.findById(commentRequest.getTaskId())
                .orElseThrow(()->new IllegalArgumentException("테스크가 없습니다."));
        List<Comment> comments = commentRepository.findByTaskId(task.getId());
        return comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 코멘트 업데이트
     * @param id
     * @param request
     * @return
     */
    public CommentResponse updateComment(Long id, CommentRequest request){
        Comment comment = Comment.builder()
                .id(id)
                .content(request.getContent())
                .build();
        commentRepository.save(comment);
        return CommentResponse.from(comment);
    }
}
