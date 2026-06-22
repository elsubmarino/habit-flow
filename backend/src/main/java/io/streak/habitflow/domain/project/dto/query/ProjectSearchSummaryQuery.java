package io.streak.habitflow.domain.project.dto.query;

public record ProjectSearchSummaryQuery(
        Long id,
        String name,
        String color,
        Long sortOrder
) {
}
