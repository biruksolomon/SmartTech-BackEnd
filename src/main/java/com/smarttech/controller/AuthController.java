package com.smarttech.controller;

import com.smarttech.dto.request.LoginRequest;
import com.smarttech.dto.request.UserRegistrationRequest;
import com.smarttech.dto.response.AuthResponse;
import com.smarttech.service.AuthService;
import com.smarttech.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final SmsService smsService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register new customer account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate token")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh JWT token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7); // Remove "Bearer " prefix
        AuthResponse response = authService.refreshToken(jwt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-phone-otp")
    @Operation(summary = "Send phone OTP", description = "Send OTP to phone number for verification")
    public ResponseEntity<String> sendPhoneOtp(@RequestParam String phoneNumber) {
        smsService.sendOtp(phoneNumber);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/verify-phone-otp")
    @Operation(summary = "Verify phone OTP", description = "Verify OTP code for phone number")
    public ResponseEntity<String> verifyPhoneOtp(@RequestParam String phoneNumber, @RequestParam String otp) {
        Boolean verified = smsService.verifyOtp(phoneNumber, otp);
        if (verified) {
            authService.verifyPhone(phoneNumber);
            return ResponseEntity.ok("Phone verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid OTP");
        }
    }

    @PostMapping("/send-email-verification")
    @Operation(summary = "Send email verification code", description = "Send verification code to email")
    public ResponseEntity<String> sendEmailVerification(@RequestParam String email) {
        authService.sendEmailVerificationCode(email);
        return ResponseEntity.ok("Verification code sent successfully");
    }

    @PostMapping("/verify-email-code")
    @Operation(summary = "Verify email code", description = "Verify email verification code")
    public ResponseEntity<String> verifyEmailCode(@RequestParam String email, @RequestParam String code) {
        Boolean verified = authService.verifyEmailCode(email, code);
        if (verified) {
            return ResponseEntity.ok("Email verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid verification code");
        }
    }
}
