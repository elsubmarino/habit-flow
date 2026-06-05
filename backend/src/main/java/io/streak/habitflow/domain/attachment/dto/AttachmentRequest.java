package io.streak.habitflow.domain.attachment.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRequest {
    private String fileUrl;
    private String originalFileName;
    private Long taskId;
}
