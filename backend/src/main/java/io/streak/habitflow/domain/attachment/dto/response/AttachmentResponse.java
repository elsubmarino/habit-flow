package io.streak.habitflow.domain.attachment.dto.response;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import lombok.*;

@Builder
public record AttachmentResponse(
        String fileUrl,
        String originalFileName
) {
    public static AttachmentResponse from(Attachment attachment) {
        return  AttachmentResponse.builder()
                .fileUrl(attachment.getFileUrl())
                .originalFileName(attachment.getOriginalFileName())
                .build();
    }
}
