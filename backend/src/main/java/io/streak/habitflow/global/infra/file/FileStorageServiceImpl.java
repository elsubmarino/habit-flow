package io.streak.habitflow.global.infra.file;

import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    @Value("${file.upload.dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    @Override
    public StoredFile upload(MultipartFile file) {
        if(file.isEmpty()){
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        try{
            Path uploadPath = Paths.get(uploadDir)
                    .toAbsolutePath().normalize();
            if(!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED.contains(contentType)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
            String ext = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                default -> ".webp";
            };
            String saveFileName = UUID.randomUUID() + ext;

            Path targetLocation = uploadPath.resolve(saveFileName);
            file.transferTo(targetLocation.toFile());

            String fileUrl = "/uploads/" + saveFileName;



            return StoredFile.builder()
                    .originalFileName(originalFileName)
                    .savedFileName(saveFileName)
                    .fileUrl(fileUrl)
                    .build();


        }catch(IOException e){
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED, "파일 저장 중 오류가 발생했습니다.", e);
        }
    }
}
