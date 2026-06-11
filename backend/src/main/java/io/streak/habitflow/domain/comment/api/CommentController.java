package io.streak.habitflow.domain.comment.api;

import io.streak.habitflow.domain.comment.dto.request.CommentRequest;
import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.global.security.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "댓글 생성")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "댓글 성공")})
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart(value="file",required = false) MultipartFile file,
            @RequestPart("commentRequest") @Valid CommentRequest commentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body( commentService.createComment(commentRequest,file,userPrincipal.getMemberId()));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 얿데이트")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "댓글 업데이트 성공")})
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long commentId,
                                                         @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @RequestBody CommentRequest commentRequest) {
        return ResponseEntity.ok(commentService.updateComment(commentId, commentRequest, userPrincipal.getMemberId()));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "댓글 삭제 성공")})
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        commentService.deleteComment(commentId,userPrincipal.getMemberId());
        return ResponseEntity.noContent().build();
    }


}
