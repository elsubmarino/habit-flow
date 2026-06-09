package io.streak.habitflow.domain.task.dto.request;

import io.streak.habitflow.domain.task.type.TaskPriorityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String name;
    private String description;
    private LocalDateTime dueDate;
    private TaskPriorityType taskPriorityType;
    private Long projectId;
    private Long parentId;

    @Builder.Default
    private List<Long> labelIds = new ArrayList<>();
}
