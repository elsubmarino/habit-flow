package io.streak.habitflow.domain.file.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FileDto {
    private String originalFileName;
    private String savedFileName;
    private String fileUrl;
}
