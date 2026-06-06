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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value="file",required = false) MultipartFile file,
            @RequestPart("commentRequest") @Valid CommentRequest commentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body( commentService.createComment(commentRequest,file,userDetails));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestBody CommentRequest commentRequest) {
        return ResponseEntity.ok(commentService.updateComment(id, commentRequest, userDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,@AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }


}
