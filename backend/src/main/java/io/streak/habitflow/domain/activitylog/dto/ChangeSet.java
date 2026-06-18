package io.streak.habitflow.domain.activitylog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChangeSet (
        @Schema(description = "대상필들", examples = {"name","description"})
        String field,
        @Schema(description = "변경이전 정보")
        String from,
        @Schema(description = "변경이후 정보")
        String to
){}
