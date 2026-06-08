package io.streak.habitflow.global.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrollResponse<T> {
    private List<T> content;
    private boolean hasNext;
    private Long nextCursor;
}
