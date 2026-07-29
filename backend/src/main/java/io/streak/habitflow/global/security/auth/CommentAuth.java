package io.streak.habitflow.global.security.auth;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.domain.comment.repository.CommentRepository;
import io.streak.habitflow.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("commentAuth")
@RequiredArgsConstructor
public class CommentAuth {
    private final CommentRepository commentRepository;

    public boolean canAccess(UUID publicCommentId){
        Comment comment = commentRepository.getOrThrowByPublicId(publicCommentId);
        return comment.getMember().getId().equals(SecurityUtils.currentMemberId());
    }
}
