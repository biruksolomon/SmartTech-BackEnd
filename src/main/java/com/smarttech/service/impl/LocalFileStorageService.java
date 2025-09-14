package com.smarttech.service.impl;

import com.smarttech.config.StorageProperties;
import com.smarttech.exception.FileStorageException;
import com.smarttech.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service("localFileStorageService")
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties storageProperties;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Value("${server.port:9090}")
    private String serverPort;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        try {
            Path uploadPath = Paths.get(storageProperties.getLocal().getBaseDirectory(), folder);

            if (storageProperties.getLocal().isCreateDirectories()) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String fileName = generateFileName(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(fileName);

            // Copy file to the target location
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = String.format("%s/%s/%s",
                    storageProperties.getLocal().getUrlPattern(), folder, fileName);

            log.info("File uploaded locally: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload file locally: {}", file.getOriginalFilename(), e);
            throw new FileStorageException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public List<String> uploadMultipleFiles(List<MultipartFile> files, String folder) {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String url = uploadFile(file, folder);
                uploadedUrls.add(url);
            } catch (Exception e) {
                log.error("Failed to upload file locally: {}", file.getOriginalFilename(), e);
                // Continue with other files
            }
        }

        return uploadedUrls;
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String relativePath = extractRelativePathFromUrl(fileUrl);
            Path filePath = Paths.get(storageProperties.getLocal().getBaseDirectory(), relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted locally: {}", fileUrl);
            }

        } catch (IOException e) {
            log.error("Failed to delete file locally: {}", fileUrl, e);
            throw new FileStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        return String.format("%s/%s", storageProperties.getLocal().getUrlPattern(), fileName);
    }

    @Override
    public boolean fileExists(String fileName) {
        Path filePath = Paths.get(storageProperties.getLocal().getBaseDirectory(), fileName);
        return Files.exists(filePath);
    }

    public String uploadPdfBytes(byte[] pdfBytes, String fileName, String folder) {
        try {
            Path uploadPath = Paths.get(storageProperties.getLocal().getBaseDirectory(), folder);

            if (storageProperties.getLocal().isCreateDirectories()) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, pdfBytes);

            String fileUrl = String.format("%s/%s/%s",
                    storageProperties.getLocal().getUrlPattern(), folder, fileName);

            log.info("PDF uploaded locally: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload PDF locally: {}", fileName, e);
            throw new FileStorageException("Failed to upload PDF: " + e.getMessage());
        }
    }

    public String uploadPdfStream(InputStream inputStream, String fileName, String folder) {
        try {
            Path uploadPath = Paths.get(storageProperties.getLocal().getBaseDirectory(), folder);

            if (storageProperties.getLocal().isCreateDirectories()) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = String.format("%s/%s/%s",
                    storageProperties.getLocal().getUrlPattern(), folder, fileName);

            log.info("PDF uploaded locally: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload PDF stream locally: {}", fileName, e);
            throw new FileStorageException("Failed to upload PDF: " + e.getMessage());
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private String extractRelativePathFromUrl(String fileUrl) {
        // Extract relative path from local URL
        String baseUrl = storageProperties.getLocal().getUrlPattern();
        return fileUrl.replace(baseUrl + "/", "");
    }
}
