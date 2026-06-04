package io.streak.habitflow.global.infra.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileStorageServiceImpl implements FileStorageService {
    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public FileDto upload(MultipartFile file) {
        if(file.isEmpty()){
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        try{
            Path uploadPath = Paths.get(uploadDir)
                    .toAbsolutePath().normalize();
            if(!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String extension =  originalFileName.substring(originalFileName.lastIndexOf("."));
            String saveFileName = UUID.randomUUID().toString() + "." + extension;

            Path targetLocation = uploadPath.resolve(saveFileName);
            file.transferTo(targetLocation.toFile());

            String fileUrl = "/uploads/" + saveFileName;

            return FileDto.builder()
                    .originalFileName(originalFileName)
                    .savedFileName(saveFileName)
                    .fileUrl(fileUrl)
                    .build();


        }catch(IOException e){
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.",e);
        }
    }
}
