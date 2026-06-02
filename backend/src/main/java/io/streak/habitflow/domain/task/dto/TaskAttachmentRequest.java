package io.streak.habitflow.domain.task.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskAttachmentRequest {
    private String fileUrl;
    private String originalFileName;
    private Long taskId;
}
