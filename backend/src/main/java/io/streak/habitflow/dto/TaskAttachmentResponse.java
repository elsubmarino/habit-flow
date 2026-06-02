package io.streak.habitflow.dto;

import io.streak.habitflow.entity.TaskAttachment;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskAttachmentResponse {
    private Long id;
    private String fileUrl;
    private String originalFileName;
    private long sortOrder;

    private Long taskId;

    public static TaskAttachmentResponse from(TaskAttachment taskAttachment) {
        TaskAttachmentResponseBuilder taskAttachmentResponseBuilder =
                TaskAttachmentResponse.builder()
                        .id(taskAttachment.getId())
                        .fileUrl(taskAttachment.getFileUrl())
                        .originalFileName(taskAttachment.getOriginalFileName())
                        .sortOrder(taskAttachment.getSortOrder());

        if(taskAttachment.getTask() != null){
            taskAttachmentResponseBuilder.taskId(taskAttachment.getTask().getId());
        }

        return  taskAttachmentResponseBuilder.build();
    }
}
