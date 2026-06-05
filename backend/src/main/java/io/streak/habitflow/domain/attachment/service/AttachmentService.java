package io.streak.habitflow.domain.attachment.service;

import io.streak.habitflow.domain.attachment.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
}
