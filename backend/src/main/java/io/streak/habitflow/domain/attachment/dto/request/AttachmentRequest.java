package io.streak.habitflow.domain.attachment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRequest {
    private String fileUrl;
    private String originalFileName;
    private Long taskId;
}
