package io.streak.habitflow.global.infra.file;

import lombok.Builder;

@Builder
public record FileDto (
    String originalFileName,
    String savedFileName,
    String fileUrl
){}
