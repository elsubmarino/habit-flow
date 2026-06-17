package io.streak.habitflow.domain.label.dto.query;

public record LabelSummaryQuery(
        Long id,
        String name,
        long sortOrder,
        boolean favorite,
        String color
) {}
