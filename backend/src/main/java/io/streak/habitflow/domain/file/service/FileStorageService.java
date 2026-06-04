package io.streak.habitflow.domain.file.service;

import io.streak.habitflow.domain.file.dto.FileDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileDto upload(MultipartFile file);
}
