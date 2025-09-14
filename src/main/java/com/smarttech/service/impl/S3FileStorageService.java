package com.smarttech.service.impl;

import com.smarttech.config.StorageProperties;
import com.smarttech.exception.FileStorageException;
import com.smarttech.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String key = folder + "/" + fileName;

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize());

            if (storageProperties.getS3().getServerSideEncryption() != null) {
                requestBuilder.serverSideEncryption(ServerSideEncryption.fromValue(storageProperties.getS3().getServerSideEncryption()));
            }

            PutObjectRequest putObjectRequest = requestBuilder.build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    storageProperties.getS3().getBucketName(),
                    storageProperties.getS3().getRegion(),
                    key);

            log.info("File uploaded successfully to S3: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", file.getOriginalFilename(), e);
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
                log.error("Failed to upload file to S3: {}", file.getOriginalFilename(), e);
                // Continue with other files, don't fail the entire batch
            }
        }

        return uploadedUrls;
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: {}", fileUrl);

        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", fileUrl, e);
            throw new FileStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                storageProperties.getS3().getBucketName(),
                storageProperties.getS3().getRegion(),
                fileName);
    }

    @Override
    public boolean fileExists(String fileName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucketName())
                    .key(fileName)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence in S3: {}", fileName, e);
            return false;
        }
    }

    public String uploadPdfBytes(byte[] pdfBytes, String fileName, String folder) {
        try {
            String key = folder + "/" + fileName;

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucketName())
                    .key(key)
                    .contentType("application/pdf")
                    .contentLength((long) pdfBytes.length);

            if (storageProperties.getS3().getServerSideEncryption() != null) {
                requestBuilder.serverSideEncryption(ServerSideEncryption.fromValue(storageProperties.getS3().getServerSideEncryption()));
            }

            PutObjectRequest putObjectRequest = requestBuilder.build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(pdfBytes));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    storageProperties.getS3().getBucketName(),
                    storageProperties.getS3().getRegion(),
                    key);

            log.info("PDF uploaded successfully to S3: {}", fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("Failed to upload PDF bytes to S3: {}", fileName, e);
            throw new FileStorageException("Failed to upload PDF: " + e.getMessage());
        }
    }

    public String uploadPdfStream(InputStream inputStream, String fileName, String folder) {
        try {
            String key = folder + "/" + fileName;

            // Convert InputStream to byte array for S3 upload
            byte[] pdfBytes = inputStream.readAllBytes();

            return uploadPdfBytes(pdfBytes, fileName, folder);

        } catch (IOException e) {
            log.error("Failed to upload PDF stream to S3: {}", fileName, e);
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

    private String extractKeyFromUrl(String fileUrl) {
        // Extract key from S3 URL format: https://bucket.s3.region.amazonaws.com/key
        String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/",
                storageProperties.getS3().getBucketName(),
                storageProperties.getS3().getRegion());
        return fileUrl.replace(baseUrl, "");
    }
}
