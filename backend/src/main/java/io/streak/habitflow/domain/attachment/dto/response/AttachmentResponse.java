package io.streak.habitflow.domain.attachment.dto.response;

import io.streak.habitflow.domain.attachment.entity.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public final class AttachmentResponse {
    @Builder
    public record Detail(
            @Schema(description = "파일이 저장된 대상 URL 정보")
            String fileUrl,
            @Schema(description = "원본 파일 이름")
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
