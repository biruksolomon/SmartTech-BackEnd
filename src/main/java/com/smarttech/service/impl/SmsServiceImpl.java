package com.smarttech.service.impl;

import com.smarttech.entity.User;
import com.smarttech.repository.UserRepository;
import com.smarttech.service.SmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${spring.application.name:SmartTech}")
    private String appName;

    @Value("${afromessage.api.base-url:https://api.afromessage.com}")
    private String afroMessageBaseUrl;

    @Value("${afromessage.api.token:}")
    private String afroMessageToken;

    @Value("${afromessage.api.sender-name:SmartTech}")
    private String afroMessageSenderName;

    @Value("${afromessage.api.identifier-id:}")
    private String afroMessageIdentifierId;

    @Value("${afromessage.api.callback-url:}")
    private String afroMessageCallbackUrl;

    @Value("${afromessage.api.enabled:true}")
    private boolean afroMessageEnabled;

    @Value("${afromessage.api.timeout:30000}")
    private int afroMessageTimeout;

    @Value("${app.sms.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${app.sms.retry-attempts:3}")
    private int retryAttempts;

    @Value("${app.sms.retry-delay:1000}")
    private long retryDelay;

    @Value("${app.otp.template}")
    private String otpTemplate;

    @Value("${app.otp.afromessage.code-length:6}")
    private int otpCodeLength;

    @Value("${app.otp.afromessage.code-type:0}")
    private int otpCodeType;

    @Value("${app.otp.afromessage.ttl:300}")
    private int otpTtl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // Track API failures to automatically enable mock mode
    private boolean apiFailureDetected = false;

    @Autowired
    public SmsServiceImpl(UserRepository userRepository) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(10000))
                .build();
        this.objectMapper = new ObjectMapper();
        this.userRepository = userRepository;
    }

    private void storeOtp(String phoneNumber, String otp) {
        try {
            storeOtpInDatabase(phoneNumber, otp);
            log.info("✅ OTP stored in database for phone: {}", phoneNumber);
        } catch (Exception e) {
            log.error("❌ Critical: Failed to store OTP in database for phone: {}", phoneNumber, e);
            throw new RuntimeException("Failed to store OTP", e);
        }
    }

    private String getStoredOtp(String phoneNumber) {
        try {
            String otp = getOtpFromDatabase(phoneNumber);
            if (otp != null) {
                log.debug("✅ OTP retrieved from database for phone: {}", phoneNumber);
                return otp;
            }
        } catch (Exception e) {
            log.error("❌ Failed to retrieve OTP from database: {}", e.getMessage());
        }
        return null;
    }

    private void removeOtp(String phoneNumber) {
        try {
            removeOtpFromDatabase(phoneNumber);
            log.debug("✅ OTP data removed from database for phone: {}", phoneNumber);
        } catch (Exception e) {
            log.error("❌ Failed to remove OTP from database: {}", e.getMessage());
        }
    }

    private void storeOtpInDatabase(String phoneNumber, String otp) {
        try {
            Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setPhoneOtp(otp);
                user.setPhoneOtpExpiry(LocalDateTime.now().plusSeconds(otpTtl));
                user.setPhoneOtpLastSent(LocalDateTime.now());
                userRepository.save(user);
                log.debug("✅ OTP stored in database for phone: {}", phoneNumber);
            } else {
                log.warn("⚠️ Could not store OTP in database: user not found for phone {}", phoneNumber);
            }
        } catch (Exception e) {
            log.error("❌ Failed to store OTP in database for phone {}: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    private String getOtpFromDatabase(String phoneNumber) {
        try {
            Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getPhoneOtp() != null &&
                        user.getPhoneOtpExpiry() != null &&
                        user.getPhoneOtpExpiry().isAfter(LocalDateTime.now())) {
                    return user.getPhoneOtp();
                } else if (user.getPhoneOtp() != null && user.getPhoneOtpExpiry() != null) {
                    log.debug("🕐 OTP expired in database for phone: {}", phoneNumber);
                    removeOtpFromDatabase(phoneNumber);
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to retrieve OTP from database for phone {}: {}", phoneNumber, e.getMessage());
        }
        return null;
    }

    private void removeOtpFromDatabase(String phoneNumber) {
        try {
            Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setPhoneOtp(null);
                user.setPhoneOtpExpiry(null);
                user.resetOtpAttempts();
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("❌ Failed to remove OTP from database for phone {}: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    private String generateOtp() {
        return String.format("%0" + otpCodeLength + "d",
                secureRandom.nextInt((int) Math.pow(10, otpCodeLength)));
    }

    private boolean isRateLimited(String phoneNumber) {
        try {
            Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return user.hasExceededOtpAttempts();
            }
        } catch (Exception e) {
            log.error("❌ Failed to check rate limit from database: {}", e.getMessage());
        }
        return false;
    }

    private void incrementOtpAttempts(String phoneNumber) {
        try {
            Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.incrementOtpAttempts();
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("❌ Failed to increment attempts in database: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void sendOtp(String phoneNumber) {
        try {
            if (!isValidEthiopianPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid Ethiopian phone number format. Expected: +251XXXXXXXXX");
            }

            if (isRateLimited(phoneNumber)) {
                throw new RuntimeException("Too many OTP requests. Please try again later.");
            }

            boolean useMockMode = false;

            if (useMockMode) {
                String otp = generateOtp();
                storeOtp(phoneNumber, otp);
                log.info("📱 [MOCK] SMS OTP sent to {}: {}", phoneNumber, otp);
                System.out.println("📱 [MOCK] SMS OTP sent to " + phoneNumber + ": " + otp);

                if (apiFailureDetected) {
                    log.warn("⚠️ Using mock mode due to AfroMessage API failure. Please check API credentials.");
                }
                return;
            }

            sendOtpViaAfroMessage(phoneNumber);
            log.info("✅ OTP sent successfully to: {}", phoneNumber);

        } catch (Exception e) {
            log.error("❌ Failed to send OTP to: {}", phoneNumber, e);
            incrementOtpAttempts(phoneNumber);

            if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("403"))) {
                apiFailureDetected = true;
                log.warn("🔄 AfroMessage API authentication failed. Switching to mock mode for future SMS operations.");

                try {
                    String otp = generateOtp();
                    storeOtp(phoneNumber, otp);
                    log.info("📱 [MOCK FALLBACK] SMS OTP sent to {}: {}", phoneNumber, otp);
                    System.out.println("📱 [MOCK FALLBACK] SMS OTP sent to " + phoneNumber + ": " + otp);
                    return;
                } catch (Exception mockException) {
                    log.error("❌ Even mock mode failed: {}", mockException.getMessage());
                }
            }

            throw new RuntimeException("Failed to send OTP", e);
        }
    }

    @Override
    @Transactional
    public void sendOtp(String phoneNumber, String otp) {
        try {
            if (!isValidEthiopianPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid Ethiopian phone number format. Expected: +251XXXXXXXXX");
            }

            if (isRateLimited(phoneNumber)) {
                throw new RuntimeException("Too many OTP requests. Please try again later.");
            }

            storeOtp(phoneNumber, otp);

            boolean useMockMode = mockMode || apiFailureDetected || !smsEnabled || !afroMessageEnabled;

            if (useMockMode) {
                log.info("📱 [MOCK] SMS OTP sent to {}: {}", phoneNumber, otp);
                System.out.println("📱 [MOCK] SMS OTP sent to " + phoneNumber + ": " + otp);
                return;
            }

            String message = otpTemplate
                    .replace("{appName}", appName)
                    .replace("{otp}", otp)
                    .replace("{minutes}", String.valueOf(otpTtl / 60));

            sendSmsWithRetry(phoneNumber, message );
            log.info("✅ OTP sent successfully to: {}", phoneNumber);

        } catch (Exception e) {
            log.error("❌ Failed to send OTP to: {}", phoneNumber, e);
            incrementOtpAttempts(phoneNumber);
            throw new RuntimeException("Failed to send OTP", e);
        }
    }

    @Override
    @Transactional
    public Boolean verifyOtp(String phoneNumber, String otp) {
        try {
            if (isRateLimited(phoneNumber)) {
                log.warn("⚠️ OTP verification rate limited for: {}", phoneNumber);
                return false;
            }

            String storedOtp = getStoredOtp(phoneNumber);
            if (storedOtp != null && storedOtp.equals(otp)) {
                removeOtp(phoneNumber);
                log.info("✅ OTP verified successfully for: {}", phoneNumber);
                return true;
            }

            incrementOtpAttempts(phoneNumber);
            log.info("❌ Invalid OTP for: {}", phoneNumber);
            return false;
        } catch (Exception e) {
            log.error("❌ OTP verification failed for: {}", phoneNumber, e);
            incrementOtpAttempts(phoneNumber);
            return false;
        }
    }

    @Override
    public void sendSellerWelcomeSms(String phoneNumber, String password) {
        try {
            if (!isValidEthiopianPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid Ethiopian phone number format");
            }

            String message = String.format("Welcome to %s! Your seller account password: %s. Please check your email for verification code.",
                    appName, password);

            boolean useMockMode = mockMode || apiFailureDetected || !smsEnabled || !afroMessageEnabled;

            if (useMockMode) {
                log.info("📱 [MOCK] Seller welcome SMS sent to {}: {}", phoneNumber, message);
                System.out.println("📱 [MOCK] Seller welcome SMS sent to " + phoneNumber + ": " + message);
                return;
            }

            sendSmsWithRetry(phoneNumber, message);
            log.info("✅ Seller welcome SMS sent to: {}", phoneNumber);

        } catch (Exception e) {
            log.error("❌ Failed to send seller welcome SMS to: {}", phoneNumber, e);
            throw new RuntimeException("Failed to send welcome SMS", e);
        }
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        try {
            if (!isValidEthiopianPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid Ethiopian phone number format");
            }

            boolean useMockMode = mockMode || apiFailureDetected || !smsEnabled || !afroMessageEnabled;

            if (useMockMode) {
                log.info("📱 [MOCK] SMS sent to {}: {}", phoneNumber, message);
                System.out.println("📱 [MOCK] SMS sent to " + phoneNumber + ": " + message);
                return;
            }

            sendSmsWithRetry(phoneNumber, message);
            log.info("✅ SMS sent successfully to: {}", phoneNumber);

        } catch (Exception e) {
            log.error("❌ Failed to send SMS to: {}", phoneNumber, e);
            throw new RuntimeException("SMS delivery failed", e);
        }
    }

    @Override
    public void sendMessage(String phoneNumber, String message) {
        try {
            sendSms(phoneNumber, message);
        } catch (Exception e) {
            log.error("❌ Failed to send system SMS to: {}", phoneNumber, e);
            throw new RuntimeException("System SMS delivery failed", e);
        }
    }

    @Override
    public void sendBulkSms(List<String> phoneNumbers, String message, String campaignName) {
        try {
            boolean useMockMode = mockMode || apiFailureDetected || !smsEnabled || !afroMessageEnabled;

            if (useMockMode) {
                log.info("📱 [MOCK] Bulk SMS sent to {} recipients: {}", phoneNumbers.size(), message);
                System.out.println("📱 [MOCK] Bulk SMS sent to " + phoneNumbers.size() + " recipients: " + message);
                return;
            }

            sendBulkSmsViaAfroMessage(phoneNumbers, message, campaignName);
            log.info("✅ Bulk SMS sent successfully to {} recipients", phoneNumbers.size());

        } catch (Exception e) {
            log.error("❌ Failed to send bulk SMS to {} recipients", phoneNumbers.size(), e);
            throw new RuntimeException("Bulk SMS delivery failed", e);
        }
    }

    private void sendOtpViaAfroMessage(String phoneNumber) throws Exception {
        String url = afroMessageBaseUrl + "/api/challenge";

        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?to=").append(URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8));
        urlBuilder.append("&len=").append(otpCodeLength);
        urlBuilder.append("&t=").append(otpCodeType);
        urlBuilder.append("&ttl=").append(otpTtl);

        if (!afroMessageIdentifierId.isEmpty()) {
            urlBuilder.append("&from=").append(URLEncoder.encode(afroMessageIdentifierId, StandardCharsets.UTF_8));
        }
        if (!afroMessageSenderName.isEmpty()) {
            urlBuilder.append("&sender=").append(URLEncoder.encode(afroMessageSenderName, StandardCharsets.UTF_8));
        }

        String prefix = "Your " + appName + " verification code is: ";
        String postfix = ". Valid for " + (otpTtl / 60) + " minutes. Do not share this code.";
        urlBuilder.append("&pr=").append(URLEncoder.encode(prefix, StandardCharsets.UTF_8));
        urlBuilder.append("&ps=").append(URLEncoder.encode(postfix, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlBuilder.toString()))
                .header("Authorization", "Bearer " + afroMessageToken)
                .timeout(java.time.Duration.ofMillis(afroMessageTimeout))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AfroMessage API error: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String acknowledge = responseJson.path("acknowledge").asText();

        if (!"success".equals(acknowledge)) {
            throw new RuntimeException("AfroMessage API error: " + responseJson.path("response").asText());
        }

        JsonNode responseData = responseJson.path("response");
        String code = responseData.path("code").asText();

        storeOtp(phoneNumber, code);
        log.info("📱 OTP sent via AfroMessage. MessageId: {}", responseData.path("message_id").asText());
    }

    private void sendSmsWithRetry(String phoneNumber, String message) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < retryAttempts) {
            try {
                sendSmsViaAfroMessage(phoneNumber, message);
                return;
            } catch (Exception e) {
                lastException = e;
                attempts++;

                if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("403"))) {
                    throw new RuntimeException("AfroMessage API authentication failed", e);
                }

                if (attempts < retryAttempts) {
                    log.warn("SMS send attempt {} failed for {}, retrying in {}ms", attempts, phoneNumber, retryDelay);
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("SMS retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("SMS delivery failed after " + retryAttempts + " attempts", lastException);
    }

    private void sendSmsViaAfroMessage(String phoneNumber, String message) throws Exception {
        String url = afroMessageBaseUrl + "/api/send";

        ObjectNode requestBody = JsonNodeFactory.instance.objectNode();
        requestBody.put("to", phoneNumber);
        requestBody.put("message", message+"\n Thank you For Joining Sentayehu Abebe Computer Retail Trade and maintenance, Smart Tech Maintenance and eCommerce Platform");
        requestBody.put("sender", afroMessageSenderName);

        if (afroMessageIdentifierId != null && !afroMessageIdentifierId.trim().isEmpty()) {
            requestBody.put("from", afroMessageIdentifierId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + afroMessageToken)
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofMillis(afroMessageTimeout))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AfroMessage API error: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String acknowledge = responseJson.path("acknowledge").asText();


        if (!"success".equals(acknowledge)) {
            throw new RuntimeException("AfroMessage API error: " + responseJson.path("response").asText());
        }
    }

    private void sendBulkSmsViaAfroMessage(List<String> phoneNumbers, String message, String campaignName) throws Exception {
        String url = afroMessageBaseUrl + "/api/bulk_send";

        ObjectNode requestBody = JsonNodeFactory.instance.objectNode();

        ArrayNode recipientsArray = JsonNodeFactory.instance.arrayNode();
        for (String phoneNumber : phoneNumbers) {
            recipientsArray.add(phoneNumber);
        }
        requestBody.set("to", recipientsArray);

        requestBody.put("message", message);
        requestBody.put("sender", afroMessageSenderName);

        if (campaignName != null && !campaignName.trim().isEmpty()) {
            requestBody.put("campaign", campaignName);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + afroMessageToken)
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofMillis(afroMessageTimeout))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AfroMessage Bulk API error: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String acknowledge = responseJson.path("acknowledge").asText();

        if (!"success".equals(acknowledge)) {
            throw new RuntimeException("AfroMessage Bulk API error: " + responseJson.path("response").asText());
        }
    }

    private boolean isValidEthiopianPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^\\+251[0-9]{9}$");
    }
}
