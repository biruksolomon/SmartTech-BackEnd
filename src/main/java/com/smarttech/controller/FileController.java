package com.smarttech.controller;

import com.smarttech.config.StorageProperties;
import com.smarttech.service.FileStorageService;
import com.smarttech.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Files", description = "File upload and management APIs")
public class FileController {

    private final FileStorageService fileStorageService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    @PostMapping("/upload")
    @Operation(summary = "Upload single file", description = "Upload a single file using configured storage")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        String fileUrl = fileStorageService.uploadFile(file, folder);

        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("message", "File uploaded successfully");
        response.put("storage", storageService.getStorageType());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-multiple")
    @Operation(summary = "Upload multiple files", description = "Upload multiple files using configured storage")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        List<String> fileUrls = fileStorageService.uploadMultipleFiles(files, folder);

        Map<String, Object> response = new HashMap<>();
        response.put("urls", fileUrls);
        response.put("count", fileUrls.size());
        response.put("message", "Files uploaded successfully");
        response.put("storage", storageService.getStorageType());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete file", description = "Delete file from configured storage")
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam("url") String fileUrl) {
        fileStorageService.deleteFile(fileUrl);

        Map<String, String> response = new HashMap<>();
        response.put("message", "File deleted successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{folder}/{fileName}")
    @Operation(summary = "Download file", description = "Download file from configured storage")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String folder,
            @PathVariable String fileName) {

        try {
            if ("local".equals(storageService.getStorageType())) {
                return downloadFromLocal(folder, fileName);
            } else {
                // For S3, redirect to the direct URL since files are publicly accessible
                String fileUrl = fileStorageService.getFileUrl(folder + "/" + fileName);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, fileUrl)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to download file: {}/{}", folder, fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "Download file (simple)", description = "Download file from configured storage (simple path)")
    public ResponseEntity<Resource> downloadFileSimple(@PathVariable String fileName) {

        try {
            if ("local".equals(storageService.getStorageType())) {
                return downloadFromLocal("general", fileName);
            } else {
                String fileUrl = fileStorageService.getFileUrl(fileName);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, fileUrl)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stream/{folder}/{fileName}")
    @Operation(summary = "Stream file", description = "Stream file content directly (for local storage)")
    public ResponseEntity<InputStreamResource> streamFile(
            @PathVariable String folder,
            @PathVariable String fileName) {

        if (!"local".equals(storageService.getStorageType())) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path filePath = Paths.get(storageProperties.getLocal().getBaseDirectory(), folder, fileName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            InputStreamResource resource = new InputStreamResource(fileInputStream);

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to stream file: {}/{}", folder, fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/info/{folder}/{fileName}")
    @Operation(summary = "Get file info", description = "Get file information and metadata")
    public ResponseEntity<Map<String, Object>> getFileInfo(
            @PathVariable String folder,
            @PathVariable String fileName) {

        try {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("fileName", fileName);
            fileInfo.put("folder", folder);
            fileInfo.put("storage", storageService.getStorageType());
            fileInfo.put("exists", fileStorageService.fileExists(folder + "/" + fileName));
            fileInfo.put("url", fileStorageService.getFileUrl(folder + "/" + fileName));

            if ("local".equals(storageService.getStorageType())) {
                Path filePath = Paths.get(storageProperties.getLocal().getBaseDirectory(), folder, fileName);
                if (Files.exists(filePath)) {
                    fileInfo.put("size", Files.size(filePath));
                    fileInfo.put("contentType", Files.probeContentType(filePath));
                    fileInfo.put("lastModified", Files.getLastModifiedTime(filePath).toString());
                }
            }

            return ResponseEntity.ok(fileInfo);

        } catch (Exception e) {
            log.error("Failed to get file info: {}/{}", folder, fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Storage health check", description = "Check storage service health and configuration")
    public ResponseEntity<Map<String, Object>> getStorageHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("storage", storageService.getStorageType());
        health.put("healthy", storageService.isHealthy());
        health.put("fallbackEnabled", storageProperties.getFallback().isEnabled());
        health.put("fallbackType", storageProperties.getFallback().getType());

        if ("local".equals(storageService.getStorageType())) {
            health.put("baseDirectory", storageProperties.getLocal().getBaseDirectory());
            health.put("maxFileSize", storageProperties.getLocal().getMaxFileSize());
        } else {
            health.put("bucket", storageProperties.getS3().getBucketName());
            health.put("region", storageProperties.getS3().getRegion());
            health.put("maxFileSize", storageProperties.getS3().getMaxFileSize());
        }

        return ResponseEntity.ok(health);
    }

    private ResponseEntity<Resource> downloadFromLocal(String folder, String fileName) throws MalformedURLException {
        Path filePath = Paths.get(storageProperties.getLocal().getBaseDirectory(), folder, fileName);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
