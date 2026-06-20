package io.streak.habitflow.domain.comment.api;

import io.streak.habitflow.domain.comment.dto.request.CommentRequest;
import io.streak.habitflow.domain.comment.dto.response.CommentResponse;
import io.streak.habitflow.domain.comment.service.CommentService;
import io.streak.habitflow.global.aop.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CommentResponse.Detail> createComment(
            @LoginMemberId Long loginMemberId,
            @RequestPart(value="file",required = false) MultipartFile file,
            @RequestPart("commentRequest") @Valid CommentRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body( commentService.createComment(request,file,loginMemberId));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 업데이트")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "댓글 업데이트 성공")})
    public ResponseEntity<CommentResponse.Detail> updateComment(@PathVariable Long commentId,
                                                 @LoginMemberId Long loginMemberId,
                                                 @RequestBody CommentRequest.Update request) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request, loginMemberId));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "댓글 삭제 성공")})
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                              @LoginMemberId Long loginMemberId) {
        commentService.deleteComment(commentId,loginMemberId);
        return ResponseEntity.noContent().build();
    }


}
