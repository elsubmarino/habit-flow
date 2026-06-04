package io.streak.habitflow.domain.comment.repository;

import io.streak.habitflow.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
