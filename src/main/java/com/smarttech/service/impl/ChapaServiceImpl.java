package com.smarttech.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttech.dto.request.PaymentRequestDTO;
//import com.smarttech.dto.SubAccountRequestDTO;
import com.smarttech.event.PaymentFailureEvent;
import com.smarttech.event.PaymentSuccessEvent;
import com.smarttech.event.TransferFailureEvent;
import com.smarttech.event.TransferSuccessEvent;
import com.smarttech.exception.ResourceNotFoundException;
import com.smarttech.entity.AccountInfo;
import com.smarttech.repository.AccountInfoRepository;
import com.smarttech.service.ChapaService;
import com.yaphet.chapa.Chapa;
import com.yaphet.chapa.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChapaServiceImpl implements ChapaService {

    private final AccountInfoRepository accountInfoRepository;
    private volatile Chapa chapa;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${chapa.webhook.secret}")
    private String webhookSecret;

    @Value("${chapa.secret-key}")
    private String chapaSecretKey;



    @PostConstruct
    public void init() {
        if (chapaSecretKey == null || chapaSecretKey.trim().isEmpty()) {
            throw new IllegalStateException("Chapa secret key is not configured");
        }
        this.chapa = new Chapa(chapaSecretKey);
        log.info("Chapa service initialized successfully");
    }

    private Chapa getChapaInstance() {
        if (chapa == null) {
            throw new IllegalStateException("Chapa service not initialized");
        }
        return chapa;
    }

    @Override
    public InitializeResponseData initializePayment(PaymentRequestDTO req) {
        log.info("Initializing Chapa payment for amount: {} ETB, reference: {}", req.getAmount(), req.getTxRef());

        Customization customization = new Customization()
                .setTitle("STMAE")
                .setDescription("Complete your purchase From Sentayehu Abebe")
                .setLogo("https://smarttech.com/logo.png");

        PostData postData = new PostData()
                .setAmount(req.getAmount())
                .setCurrency(req.getCurrency())
                .setFirstName(req.getFirstName())
                .setLastName(req.getLastName())
                .setEmail(req.getEmail())
                .setTxRef(req.getTxRef())
                .setCallbackUrl(req.getCallbackUrl())
                .setCustomization(customization);

        try {
            InitializeResponseData response = getChapaInstance().initialize(postData);
            log.info("Chapa payment initialized successfully for reference: {}", req.getTxRef());
            return response;
        } catch (Throwable e) {
            log.error("Chapa payment initialization failed for reference: {}", req.getTxRef(), e);
            throw new RuntimeException("Chapa payment initialization failed: " + e.getMessage(), e);
        }
    }

/*    @Override
    public SubAccountResponseData createSubAccount(SubAccountRequestDTO req) {
        log.info("Creating Chapa subaccount for business: {}", req.getBusinessName());

        SubAccount subAccount = new SubAccount()
                .setBusinessName(req.getBusinessName())
                .setAccountName(req.getAccountName())
                .setAccountNumber(req.getAccountNumber())
                .setBankCode(req.getBankCode())
                .setSplitType(req.getSplitType())
                .setSplitValue(req.getSplitValue());

        try {
            SubAccountResponseData response = getChapaInstance().createSubAccount(subAccount);
            log.info("Chapa subaccount created successfully for business: {}", req.getBusinessName());
            return response;
        } catch (Throwable e) {
            log.error("Subaccount creation failed for business: {}", req.getBusinessName(), e);
            throw new RuntimeException("Subaccount creation failed: " + e.getMessage(), e);
        }
    }*/

   /* @Override
    public boolean processSellerPayout(Long selectedAccountId, BigDecimal amount, String payoutReference) {
        log.info("Processing seller payout - SubAccount: {}, Amount: {}, Reference: {}",
                selectedAccountId, amount, payoutReference);

        if (selectedAccountId == null) {
            log.error("Selected account ID cannot be null");
            return false;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid payout amount: {}", amount);
            return false;
        }

        if (payoutReference == null || payoutReference.trim().isEmpty()) {
            log.error("Payout reference cannot be null or empty");
            return false;
        }

        AccountInfo selectedAccount = accountInfoRepository.findById(selectedAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Selected account not found !"));

        // Validate account details
        if (selectedAccount.getAccountName() == null || selectedAccount.getAccountName().trim().isEmpty()) {
            log.error("Account name is missing for account ID: {}", selectedAccountId);
            return false;
        }

        if (selectedAccount.getAccountNumber() == null || selectedAccount.getAccountNumber().trim().isEmpty()) {
            log.error("Account number is missing for account ID: {}", selectedAccountId);
            return false;
        }

        if (selectedAccount.getBankCode() == null || selectedAccount.getBankCode().trim().isEmpty()) {
            log.error("Bank code is missing for account ID: {}", selectedAccountId);
            return false;
        }

        try {
            // Prepare transfer request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("account_name", selectedAccount.getAccountName().trim());
            payload.put("account_number", selectedAccount.getAccountNumber().trim());
            payload.put("amount", amount.toString());
            payload.put("currency", "ETB");
            payload.put("reference", payoutReference.trim());
            payload.put("bank_code", selectedAccount.getBankCode().trim());

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + chapaSecretKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Make the transfer request
            ResponseEntity<String> response = restTemplate.postForEntity(CHAPA_TRANSFER_URL, request, String.class);
            log.info("Chapa transfer response: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Seller payout processed successfully - Reference: {}", payoutReference);
                return true;
            } else {
                log.error("Chapa payout failed with status: {} - Response: {}",
                        response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Seller payout failed - Reference: {}", payoutReference, e);
            return false;
        }
    }*/

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || payload.trim().isEmpty()) {
            log.error("Webhook payload cannot be null or empty");
            return false;
        }

        if (signature == null || signature.trim().isEmpty()) {
            log.error("Webhook signature cannot be null or empty");
            return false;
        }

        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            log.error("Webhook secret is not configured - rejecting webhook");
            return false;
        }

        try {
            String expectedSignature = generateHmacSha256(payload, webhookSecret);
            boolean isValid = expectedSignature.equals(signature.trim());

            if (!isValid) {
                log.warn("Webhook signature verification failed. Expected: {}, Received: {}",
                        expectedSignature, signature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    @Override
    public void processWebhookPayment(String payload) {
        try {
            JsonNode webhookData = objectMapper.readTree(payload);
            String event = webhookData.get("event").asText();
            String status = webhookData.get("status").asText();
            String txRef = webhookData.get("tx_ref").asText();

            log.info("Processing webhook payment - Event: {}, Status: {}, TxRef: {}", event, status, txRef);

            if ("charge.success".equals(event) && "success".equalsIgnoreCase(status)) {
                // Publish event instead of direct call
                eventPublisher.publishEvent(new PaymentSuccessEvent(txRef, payload));
            } else if ("charge.failed".equals(event) || "charge.cancelled".equals(event)) {
                String reason = webhookData.has("message") ? webhookData.get("message").asText() : "Payment failed";
                eventPublisher.publishEvent(new PaymentFailureEvent(txRef, reason));
            }
        } catch (Exception e) {
            log.error("Failed to process webhook payment", e);
            throw new RuntimeException("Failed to process webhook payment: " + e.getMessage(), e);
        }
    }

    @Override
    public void processWebhookTransfer(String payload) {
        try {
            JsonNode webhookData = objectMapper.readTree(payload);
            String event = webhookData.get("event").asText();
            String status = webhookData.get("status").asText();
            String reference = webhookData.get("reference").asText();

            log.info("Processing webhook transfer - Event: {}, Status: {}, Reference: {}", event, status, reference);

            if ("payout.success".equals(event) && "success".equalsIgnoreCase(status)) {
                eventPublisher.publishEvent(new TransferSuccessEvent(reference, payload));
            } else if ("payout.failed".equals(event) || "payout.cancelled".equals(event)) {
                String reason = webhookData.has("message") ? webhookData.get("message").asText() : "Transfer failed";
                eventPublisher.publishEvent(new TransferFailureEvent(reference, reason));
            }
        } catch (Exception e) {
            log.error("Failed to process webhook transfer", e);
            throw new RuntimeException("Failed to process webhook transfer: " + e.getMessage(), e);
        }
    }

    private String generateHmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
