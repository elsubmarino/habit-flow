package io.streak.habitflow.domain.label.dto.query;

import java.util.UUID;

public record LabelSummaryQuery(
        Long id,
        UUID publicId,
        String name,
        String color,
        long sortOrder
) {}
