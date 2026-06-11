package io.streak.habitflow.domain.task.dto.request;

import io.streak.habitflow.domain.task.type.TaskPriorityType;
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
public class TaskUpdateRequest {
    private String name;
    private String description;

    @Builder.Default
    private List<Long> labelIds = new ArrayList<>();
}
