package io.streak.habitflow.domain.task.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskPriorityType {
    P1(1,"우선순위1"),
    P2(2,"우선순위2"),
    P3(3,"우선순위3"),
    P4(4,"우선순위4");
    private final int level;
    private final String description;
}
