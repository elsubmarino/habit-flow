package io.streak.habitflow.domain.task.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskSearchCondition {
    private String filterType;
}
