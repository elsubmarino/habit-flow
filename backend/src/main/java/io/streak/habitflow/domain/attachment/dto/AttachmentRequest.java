package io.streak.habitflow.domain.attachment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentRequest {
    private String fileUrl;
    private String originalFileName;
    private Long taskId;
}
