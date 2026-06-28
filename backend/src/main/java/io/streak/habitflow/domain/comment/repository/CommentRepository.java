package io.streak.habitflow.domain.comment.repository;

import io.streak.habitflow.domain.comment.entity.Comment;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.attachments WHERE c.task.id = :taskId")
    List<Comment> findByTaskIdWithAttachments(@Param("taskId") Long taskId);

    default Comment getOrThrow(Long commentId){
        return findById(commentId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
}
