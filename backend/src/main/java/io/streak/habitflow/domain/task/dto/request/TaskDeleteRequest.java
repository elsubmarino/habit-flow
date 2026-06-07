package io.streak.habitflow.domain.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDeleteRequest {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;
    private String description;
    private String priorityType;
    private LocalDateTime dueDate;
    private Long projectId;
    private Long parentId;

    @Builder.Default
    private List<Long> labelIds = new ArrayList<>();
}
