package io.streak.habitflow.global.infra.file;

import lombok.Builder;

@Builder
public record StoredFile(
    String originalFileName,
    String savedFileName,
    String fileUrl
){}
