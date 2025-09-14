package com.smarttech.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String type = "local";
    private Providers providers = new Providers();
    private Fallback fallback = new Fallback();
    private Local local = new Local();
    private S3 s3 = new S3();
    private Map<String, String> folders;

    @Data
    public static class Providers {
        private boolean localEnabled = true;
        private boolean s3Enabled = false;
    }

    @Data
    public static class Fallback {
        private boolean enabled = true;
        private String type = "local";
    }

    @Data
    public static class Local {
        private String baseDirectory = "./uploads";
        private boolean createDirectories = true;
        private String urlPattern = "http://localhost:9090/api/v1/files/download";
        private String allowedExtensions = "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip";
        private long maxFileSize = 10485760L; // 10MB
    }

    @Data
    public static class S3 {
        private String bucketName = "smart-tech-files";
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
        private String endpoint;
        private boolean pathStyleAccess = false;
        private boolean publicRead = true;
        private String serverSideEncryption = "AES256";
        private long maxFileSize = 52428800L; // 50MB
        private int urlExpirationHours = 24;
    }
}
