package com.smarttech.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface StorageService {
    String uploadFile(MultipartFile file, String folder);
    List<String> uploadMultipleFiles(List<MultipartFile> files, String folder);
    String uploadPdfBytes(byte[] pdfBytes, String fileName, String folder);
    String uploadPdfStream(InputStream inputStream, String fileName, String folder);
    void deleteFile(String fileUrl);
    String getFileUrl(String fileName);
    boolean fileExists(String fileName);
    String getStorageType();
    boolean isHealthy();
}
