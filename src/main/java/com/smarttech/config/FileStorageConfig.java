package com.smarttech.config;

import com.smarttech.service.FileStorageService;
import com.smarttech.service.StorageService;
import com.smarttech.service.impl.LocalFileStorageService;
import com.smarttech.service.impl.S3FileStorageService;
import com.smarttech.service.impl.UnifiedStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(StorageProperties.class)
public class FileStorageConfig {

    private final StorageProperties storageProperties;

    @Bean
    @Primary
    public FileStorageService fileStorageService(
            S3FileStorageService s3FileStorageService,
            LocalFileStorageService localFileStorageService) {

        String storageType = storageProperties.getType();

        if ("s3".equals(storageType) && storageProperties.getProviders().isS3Enabled()) {
            log.info("Using S3 file storage service as primary. Bucket: {}, Region: {}",
                    storageProperties.getS3().getBucketName(),
                    storageProperties.getS3().getRegion());
            return s3FileStorageService;
        } else if ("local".equals(storageType) && storageProperties.getProviders().isLocalEnabled()) {
            log.info("Using local file storage service as primary. Directory: {}",
                    storageProperties.getLocal().getBaseDirectory());
            return localFileStorageService;
        } else {
            log.warn("Invalid storage configuration. Falling back to local storage. " +
                            "Type: {}, S3 Enabled: {}, Local Enabled: {}",
                    storageType,
                    storageProperties.getProviders().isS3Enabled(),
                    storageProperties.getProviders().isLocalEnabled());
            return localFileStorageService;
        }
    }

    @Bean
    public StorageService storageService(
            LocalFileStorageService localFileStorageService,
            S3FileStorageService s3FileStorageService) {

        log.info("Initializing unified storage service with primary: {}, fallback enabled: {}",
                storageProperties.getType(),
                storageProperties.getFallback().isEnabled());

        return new UnifiedStorageService(storageProperties, localFileStorageService, s3FileStorageService);
    }
}
