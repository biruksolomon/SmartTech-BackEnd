package com.smarttech.controller;

import com.smarttech.service.ChapaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Webhook endpoints for external services")
public class WebhookController {

    private final ChapaService chapaService;

    @Value("${chapa.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/chapa/payment")
    @Operation(summary = "Chapa payment webhook", description = "Handle Chapa payment notifications")
    public ResponseEntity<String> handleChapaPaymentWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Chapa-Signature", required = false) String chapaSignature,
            @RequestHeader(value = "x-chapa-signature", required = false) String xChapaSignature) {

        log.info("Received Chapa payment webhook");

        try {
            if (!isValidSignature(payload, chapaSignature, xChapaSignature)) {
                log.error("Invalid webhook signature received for payment webhook");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // Process webhook with signature verification
            chapaService.processWebhookPayment(payload);

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing Chapa payment webhook", e);
            return ResponseEntity.internalServerError().body("Webhook processing failed");
        }
    }

    @GetMapping("/chapa/payment")
    @Operation(summary = "Chapa payment redirect", description = "Handle Chapa payment browser redirects")
    public ResponseEntity<String> handleChapaPaymentRedirect(
            @RequestParam(value = "trx_ref", required = false) String transactionRef,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "callback", required = false) String callback) {

        log.info("Received Chapa payment redirect - trx_ref: {}, status: {}", transactionRef, status);

        try {
            // This is likely a browser redirect/callback, not a webhook notification
            // Log for monitoring but don't process as webhook
            if (transactionRef != null && status != null) {
                log.info("Payment redirect received for transaction: {} with status: {}", transactionRef, status);
            }

            // Return success response for browser redirects
            return ResponseEntity.ok("Payment redirect processed");
        } catch (Exception e) {
            log.error("Error processing Chapa payment redirect", e);
            return ResponseEntity.ok("Redirect processed with warnings");
        }
    }

    @PostMapping("/chapa/transfer")
    @Operation(summary = "Chapa transfer webhook", description = "Handle Chapa transfer notifications")
    public ResponseEntity<String> handleChapaTransferWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Chapa-Signature", required = false) String chapaSignature,
            @RequestHeader(value = "x-chapa-signature", required = false) String xChapaSignature) {

        log.info("Received Chapa transfer webhook");

        try {
            if (!isValidSignature(payload, chapaSignature, xChapaSignature)) {
                log.error("Invalid webhook signature received for transfer webhook");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // Process webhook with signature verification
            chapaService.processWebhookTransfer(payload);

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing Chapa transfer webhook", e);
            return ResponseEntity.internalServerError().body("Webhook processing failed");
        }
    }


    /**
     * Validates the webhook signature to ensure it's from Chapa
     * Checks both Chapa-Signature and x-chapa-signature headers
     */
    private boolean isValidSignature(String payload, String chapaSignature, String xChapaSignature) {
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            log.warn("Webhook secret not configured - skipping signature validation");
            return true; // Allow in development, but should be configured in production
        }

        // Check if at least one signature header is present
        if ((chapaSignature == null || chapaSignature.trim().isEmpty()) &&
                (xChapaSignature == null || xChapaSignature.trim().isEmpty())) {
            log.error("No signature provided in webhook request");
            return false;
        }

        try {
            // Calculate expected signature using hex encoding (as per Chapa docs)
            String expectedSignature = calculateHmacSha256Hex(payload, webhookSecret);

            // Check Chapa-Signature header
            if (chapaSignature != null && !chapaSignature.trim().isEmpty()) {
                String cleanChapaSignature = chapaSignature.startsWith("sha256=") ?
                        chapaSignature.substring(7) : chapaSignature;
                if (secureEquals(cleanChapaSignature, expectedSignature)) {
                    log.debug("Valid Chapa-Signature header verified");
                    return true;
                }
            }

            // Check x-chapa-signature header
            if (xChapaSignature != null && !xChapaSignature.trim().isEmpty()) {
                String cleanXChapaSignature = xChapaSignature.startsWith("sha256=") ?
                        xChapaSignature.substring(7) : xChapaSignature;
                if (secureEquals(cleanXChapaSignature, expectedSignature)) {
                    log.debug("Valid x-chapa-signature header verified");
                    return true;
                }
            }

            log.error("Signature verification failed for both headers");
            return false;

        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Calculates HMAC-SHA256 signature and returns as hex string
     */
    private String calculateHmacSha256Hex(String data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
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

    /**
     * Secure string comparison to prevent timing attacks
     */
    private boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
