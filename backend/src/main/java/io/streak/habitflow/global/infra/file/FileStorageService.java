package io.streak.habitflow.global.infra.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileDto upload(MultipartFile file);
}
