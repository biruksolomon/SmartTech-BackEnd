package com.smarttech.service.impl;

import com.smarttech.config.StorageProperties;
import com.smarttech.exception.FileStorageException;
import com.smarttech.service.FileStorageService;
import com.smarttech.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedStorageService implements StorageService {

    private final StorageProperties storageProperties;
    private final LocalFileStorageService localFileStorageService;
    private final S3FileStorageService s3FileStorageService;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        FileStorageService primaryService = getPrimaryStorageService();

        try {
            return primaryService.uploadFile(file, folder);
        } catch (Exception e) {
            log.error("Primary storage ({}) failed for file upload: {}",
                    storageProperties.getType(), e.getMessage());

            if (storageProperties.getFallback().isEnabled()) {
                return handleFallback(() -> getFallbackStorageService().uploadFile(file, folder));
            }
            throw e;
        }
    }

    @Override
    public List<String> uploadMultipleFiles(List<MultipartFile> files, String folder) {
        FileStorageService primaryService = getPrimaryStorageService();

        try {
            return primaryService.uploadMultipleFiles(files, folder);
        } catch (Exception e) {
            log.error("Primary storage ({}) failed for multiple file upload: {}",
                    storageProperties.getType(), e.getMessage());

            if (storageProperties.getFallback().isEnabled()) {
                return handleFallback(() -> getFallbackStorageService().uploadMultipleFiles(files, folder));
            }
            throw e;
        }
    }

    @Override
    public String uploadPdfBytes(byte[] pdfBytes, String fileName, String folder) {
        try {
            if ("s3".equals(storageProperties.getType()) && storageProperties.getProviders().isS3Enabled()) {
                return s3FileStorageService.uploadPdfBytes(pdfBytes, fileName, folder);
            } else {
                return localFileStorageService.uploadPdfBytes(pdfBytes, fileName, folder);
            }
        } catch (Exception e) {
            log.error("Primary storage ({}) failed for PDF bytes upload: {}",
                    storageProperties.getType(), e.getMessage());

            if (storageProperties.getFallback().isEnabled()) {
                return handleFallback(() -> {
                    if ("local".equals(storageProperties.getFallback().getType())) {
                        return localFileStorageService.uploadPdfBytes(pdfBytes, fileName, folder);
                    } else {
                        return s3FileStorageService.uploadPdfBytes(pdfBytes, fileName, folder);
                    }
                });
            }
            throw e;
        }
    }

    @Override
    public String uploadPdfStream(InputStream inputStream, String fileName, String folder) {
        try {
            if ("s3".equals(storageProperties.getType()) && storageProperties.getProviders().isS3Enabled()) {
                return s3FileStorageService.uploadPdfStream(inputStream, fileName, folder);
            } else {
                return localFileStorageService.uploadPdfStream(inputStream, fileName, folder);
            }
        } catch (Exception e) {
            log.error("Primary storage ({}) failed for PDF stream upload: {}",
                    storageProperties.getType(), e.getMessage());

            if (storageProperties.getFallback().isEnabled()) {
                return handleFallback(() -> {
                    if ("local".equals(storageProperties.getFallback().getType())) {
                        return localFileStorageService.uploadPdfStream(inputStream, fileName, folder);
                    } else {
                        return s3FileStorageService.uploadPdfStream(inputStream, fileName, folder);
                    }
                });
            }
            throw e;
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        FileStorageService primaryService = getPrimaryStorageService();

        try {
            primaryService.deleteFile(fileUrl);
        } catch (Exception e) {
            log.error("Primary storage ({}) failed for file deletion: {}",
                    storageProperties.getType(), e.getMessage());

            if (storageProperties.getFallback().isEnabled()) {
                handleFallback(() -> {
                    getFallbackStorageService().deleteFile(fileUrl);
                    return null;
                });
            } else {
                throw e;
            }
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        return getPrimaryStorageService().getFileUrl(fileName);
    }

    @Override
    public boolean fileExists(String fileName) {
        try {
            return getPrimaryStorageService().fileExists(fileName);
        } catch (Exception e) {
            log.error("Primary storage ({}) failed for file existence check: {}",
                    storageProperties.getType(), e.getMessage());

            if (storageProperties.getFallback().isEnabled()) {
                return handleFallback(() -> getFallbackStorageService().fileExists(fileName));
            }
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return storageProperties.getType();
    }

    @Override
    public boolean isHealthy() {
        try {
            FileStorageService primaryService = getPrimaryStorageService();
            // Simple health check - could be enhanced with actual connectivity tests
            return primaryService != null;
        } catch (Exception e) {
            log.error("Storage health check failed: {}", e.getMessage());
            return false;
        }
    }

    private FileStorageService getPrimaryStorageService() {
        String storageType = storageProperties.getType();

        if ("s3".equals(storageType) && storageProperties.getProviders().isS3Enabled()) {
            log.debug("Using S3 storage service as primary");
            return s3FileStorageService;
        } else if ("local".equals(storageType) && storageProperties.getProviders().isLocalEnabled()) {
            log.debug("Using local storage service as primary");
            return localFileStorageService;
        } else {
            throw new FileStorageException("No valid primary storage service configured. Type: " + storageType);
        }
    }

    private FileStorageService getFallbackStorageService() {
        String fallbackType = storageProperties.getFallback().getType();

        if ("s3".equals(fallbackType) && storageProperties.getProviders().isS3Enabled()) {
            log.info("Using S3 storage service as fallback");
            return s3FileStorageService;
        } else if ("local".equals(fallbackType) && storageProperties.getProviders().isLocalEnabled()) {
            log.info("Using local storage service as fallback");
            return localFileStorageService;
        } else {
            throw new FileStorageException("No valid fallback storage service configured. Type: " + fallbackType);
        }
    }

    private <T> T handleFallback(FallbackOperation<T> operation) {
        try {
            log.info("Attempting fallback to {} storage", storageProperties.getFallback().getType());
            T result = operation.execute();
            log.info("Fallback operation successful");
            return result;
        } catch (Exception fallbackException) {
            log.error("Fallback storage also failed: {}", fallbackException.getMessage());
            throw new FileStorageException("Both primary and fallback storage failed", fallbackException);
        }
    }

    @FunctionalInterface
    private interface FallbackOperation<T> {
        T execute() throws Exception;
    }
}
