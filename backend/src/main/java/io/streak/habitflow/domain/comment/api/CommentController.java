package io.streak.habitflow.domain.comment.api;

import io.streak.habitflow.domain.comment.dto.CommentRequest;
import io.streak.habitflow.domain.comment.dto.CommentResponse;
import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentController {
    private final CommentService commentService;

    /**
     * 코멘트 생성
     * @param userDetails
     * @param file
     * @param commentRequest
     * @return
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
     * 코멘트 다건 조회
     * @param userDetails
     * @param commentRequest
     * @return
     */
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestBody CommentRequest commentRequest) {
        return ResponseEntity.ok(commentService.getComments(commentRequest));
    }

    /**
     * 코멘트 업데이트
     * @param id
     * @param userDetails
     * @param commentRequest
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestBody CommentRequest commentRequest) {
        return ResponseEntity.ok(commentService.updateComment(id, commentRequest));
    }

    /**
     * 코멘트 삭제
     * @param id
     * @param userDetails
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,@AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }


}
