package com.smarttech.dto.response;

import com.smarttech.enums.CustomerTier;
import com.smarttech.enums.UserRole;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private UserRole role;
    private CustomerTier customerTier;
    private BigDecimal totalPurchases;
    private Boolean isActive;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private LocalDateTime createdAt;
}
