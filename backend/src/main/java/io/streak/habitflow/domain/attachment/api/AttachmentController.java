package io.streak.habitflow.domain.attachment.api;

import io.streak.habitflow.domain.attachment.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;
}
