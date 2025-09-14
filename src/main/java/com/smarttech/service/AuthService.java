package com.smarttech.service;

import com.smarttech.dto.request.LoginRequest;
import com.smarttech.dto.request.UserRegistrationRequest;
import com.smarttech.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(UserRegistrationRequest request);
    void logout(String token);
    AuthResponse refreshToken(String token);

    void sendEmailVerificationCode(String email);

    Boolean verifyEmailCode(String email, String code);

    void verifyPhone(String phoneNumber);
}
