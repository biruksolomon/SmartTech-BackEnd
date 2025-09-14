package com.smarttech.service;

import com.smarttech.dto.request.UserRegistrationRequest;
import com.smarttech.dto.response.UserResponse;
import com.smarttech.entity.User;
import com.smarttech.enums.CustomerTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface UserService {
    UserResponse registerUser(UserRegistrationRequest request);
    UserResponse getUserById(Long id);
    UserResponse getUserByEmail(String email);
    Page<UserResponse> getAllCustomers(Pageable pageable);
    UserResponse updateCustomerTier(Long userId, BigDecimal totalPurchases);
    void verifyEmail(String email);
    void verifyPhone(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    User findEntityById(Long id);
    User findEntityByEmail(String email);
}
