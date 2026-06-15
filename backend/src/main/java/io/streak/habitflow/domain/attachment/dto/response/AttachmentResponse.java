package io.streak.habitflow.domain.attachment.dto.response;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import lombok.Builder;

public final class AttachmentResponse {
    @Builder
    public record Detail(
            String fileUrl,
            String originalFileName
    ) {
        public static Detail from(Attachment attachment) {
            return  Detail.builder()
                    .fileUrl(attachment.getFileUrl())
                    .originalFileName(attachment.getOriginalFileName())
                    .build();
        }
    }
}
