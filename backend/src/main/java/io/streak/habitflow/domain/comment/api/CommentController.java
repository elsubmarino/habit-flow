package io.streak.habitflow.domain.comment.api;

import io.streak.habitflow.domain.comment.dto.CommentRequest;
import io.streak.habitflow.domain.comment.dto.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    /**
     * 코멘트 생성
     * @param userDetails 인증된 사용자 정보
     * @param file 첨부파일 (선택)
     * @param commentRequest 댓글 요청 정보 DTO
     * @return 댓글 응답 정보 DTO
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value="file",required = false) MultipartFile file,
            @RequestPart("commentRequest") @Valid CommentRequest commentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body( commentService.createComment(commentRequest,file,userDetails));
    }



    /**
     * 코멘트 업데이트
     * @param id 댓글 ID
     * @param userDetails 인증된 사용자 정보
     * @param commentRequest 댓글 요청 정보 DTO
     * @return 댓글 응답 정보 DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestBody CommentRequest commentRequest) {
        return ResponseEntity.ok(commentService.updateComment(id, commentRequest, userDetails));
    }

    /**
     * 코멘트 삭제
     * @param id 댓글 ID
     * @param userDetails 인증된 사용자 정보
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,@AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }


}
