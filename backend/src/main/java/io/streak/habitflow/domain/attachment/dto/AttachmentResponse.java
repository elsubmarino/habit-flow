package io.streak.habitflow.domain.attachment.dto;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private String fileUrl;
    private String originalFileName;
    private long sortOrder;

    private Long taskId;

    public static AttachmentResponse from(Attachment attachment) {
        AttachmentResponseBuilder builder =
                AttachmentResponse.builder()
                        .id(attachment.getId())
                        .fileUrl(attachment.getFileUrl())
                        .originalFileName(attachment.getOriginalFileName());

        return  builder.build();
    }
}
