package io.streak.habitflow.domain.label.dto.query;

public record LabelSummaryQuery(
        Long id,
        String name,
        String color,
        long sortOrder
) {}
