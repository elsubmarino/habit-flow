package io.streak.habitflow.domain.attachment.repository;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment,Long> {
}
