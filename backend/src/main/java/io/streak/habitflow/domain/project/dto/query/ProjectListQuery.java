package io.streak.habitflow.domain.project.dto.query;

public record ProjectListQuery(
        Long id,
        String name,
        String color,
        Long taskCount
) {}
