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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value="file",required = false) MultipartFile file,
            @RequestPart("commentRequest") @Valid CommentRequest commentRequest) {
        commentService.createComment(commentRequest,file,userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestBody CommentRequest commentRequest) {
        List<CommentResponse> commentResponses = commentService.getComments(commentRequest);
        return ResponseEntity.status(HttpStatus.OK).body(commentResponses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestBody CommentRequest commentRequest) {
        CommentResponse commentResponse = commentService.updateComment(id, commentRequest);
        return ResponseEntity.status(HttpStatus.OK).body(commentResponse);
    }


}
