package io.streak.habitflow.domain.task.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class TaskUpdateLabelRequest {
    private List<Long> labelIds;
}
