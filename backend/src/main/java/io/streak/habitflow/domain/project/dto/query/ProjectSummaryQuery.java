package io.streak.habitflow.domain.project.dto.query;

public record ProjectSummaryQuery(
        Long id,
        String name,
        String color,
        Long taskCount,
        Long sortOrder
) {}
