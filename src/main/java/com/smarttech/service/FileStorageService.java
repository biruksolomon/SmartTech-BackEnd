package com.smarttech.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String folder);
    List<String> uploadMultipleFiles(List<MultipartFile> files, String folder);
    void deleteFile(String fileUrl);
    String getFileUrl(String fileName);
    boolean fileExists(String fileName);
}
