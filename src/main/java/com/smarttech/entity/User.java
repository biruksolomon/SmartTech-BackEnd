package com.smarttech.entity;

import com.smarttech.enums.CustomerTier;
import com.smarttech.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_tier")
    private CustomerTier customerTier = CustomerTier.BRONZE;

    @Column(name = "total_purchases", precision = 10, scale = 2)
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    @Column(name = "phone_otp")
    private String phoneOtp;

    @Column(name = "phone_otp_expiry")
    private LocalDateTime phoneOtpExpiry;

    @Column(name = "phone_otp_last_sent")
    private LocalDateTime phoneOtpLastSent;

    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Column(name = "email_verification_code")
    private String emailVerificationCode;

    @Column(name = "email_verification_expiry")
    private LocalDateTime emailVerificationExpiry;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MaintenanceRequest> maintenanceRequests;

    public boolean hasExceededOtpAttempts() {
        return this.otpAttempts != null && this.otpAttempts >= 5;
    }

    public void incrementOtpAttempts() {
        this.otpAttempts = (this.otpAttempts == null) ? 1 : this.otpAttempts + 1;
    }

    public void resetOtpAttempts() {
        this.otpAttempts = 0;
    }
}
