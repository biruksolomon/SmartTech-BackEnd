package com.smarttech.service.impl;

import com.smarttech.dto.request.LoginRequest;
import com.smarttech.dto.request.UserRegistrationRequest;
import com.smarttech.dto.response.AuthResponse;
import com.smarttech.dto.response.UserResponse;
import com.smarttech.entity.User;
import com.smarttech.repository.UserRepository;
import com.smarttech.security.JwtTokenProvider;
import com.smarttech.service.AuthService;
import com.smarttech.service.EmailService;
import com.smarttech.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserResponse user = userService.getUserByEmail(request.getEmail());
        log.info("User logged in successfully: {}", request.getEmail());

        return new AuthResponse(jwt, user);
    }

    @Override
    public AuthResponse register(UserRegistrationRequest request) {
        log.info("User registration attempt for email: {}", request.getEmail());

        UserResponse user = userService.registerUser(request);
        String jwt = tokenProvider.generateTokenFromUsername(user.getEmail());

        log.info("User registered successfully: {}", request.getEmail());
        return new AuthResponse(jwt, user);
    }

    @Override
    public void logout(String token) {

        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }

    @Override
    public AuthResponse refreshToken(String token) {
        String username = tokenProvider.getUsernameFromToken(token);
        UserResponse user = userService.getUserByEmail(username);
        String newToken = tokenProvider.generateTokenFromUsername(username);

        return new AuthResponse(newToken, user);
    }

    @Override
    public void sendEmailVerificationCode(String email) {
        log.info("Sending email verification code for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Generate 6-digit verification code
        String verificationCode = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));

        // Store code with 15-minute expiry
        user.setEmailVerificationCode(verificationCode);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Send email
        emailService.sendEmailVerification(user, verificationCode);
        log.info("Email verification code sent successfully for: {}", email);
    }

    @Override
    public Boolean verifyEmailCode(String email, String code) {
        log.info("Verifying email code for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.getEmailVerificationCode() != null &&
                user.getEmailVerificationExpiry() != null &&
                user.getEmailVerificationExpiry().isAfter(LocalDateTime.now()) &&
                user.getEmailVerificationCode().equals(code)) {

            // Mark email as verified and clear verification code
            user.setEmailVerified(true);
            user.setEmailVerificationCode(null);
            user.setEmailVerificationExpiry(null);
            userRepository.save(user);

            log.info("Email verified successfully for: {}", email);
            return true;
        }

        log.warn("Invalid or expired verification code for: {}", email);
        return false;
    }

    @Override
    public void verifyPhone(String phoneNumber) {
        userService.verifyPhone(phoneNumber);
    }
}
