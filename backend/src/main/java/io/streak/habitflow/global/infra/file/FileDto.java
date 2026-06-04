package io.streak.habitflow.global.infra.file;

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
