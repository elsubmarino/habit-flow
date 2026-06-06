package io.streak.habitflow.domain.attachment.dto;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private String fileUrl;
    private String originalFileName;

    public static AttachmentResponse from(Attachment attachment) {
        return  AttachmentResponse.builder()
                .fileUrl(attachment.getFileUrl())
                .originalFileName(attachment.getOriginalFileName())
                .build();
    }
}
