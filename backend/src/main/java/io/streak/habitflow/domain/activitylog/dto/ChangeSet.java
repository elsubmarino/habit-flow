package io.streak.habitflow.domain.activitylog.dto;

public record ChangeSet (
        String field,
        String from,
        String to
){}
