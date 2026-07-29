package io.streak.habitflow.domain.project.dto.query;

import java.util.UUID;

public record ProjectSummaryQuery(
        Long id,
        UUID publicId,
        String name,
        String color,
        Long taskCount,
        Long sortOrder
) {}
