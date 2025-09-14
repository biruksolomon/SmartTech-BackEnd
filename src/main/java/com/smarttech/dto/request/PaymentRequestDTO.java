package com.smarttech.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {
    private BigDecimal amount;
    private String currency = "ETB";
    private String firstName;
    private String lastName;
    private String email;
    private String txRef;
    private String callbackUrl;
    private String subAccountId;
}
