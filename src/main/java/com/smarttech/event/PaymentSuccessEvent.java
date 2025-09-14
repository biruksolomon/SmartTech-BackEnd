package com.smarttech.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentSuccessEvent {
    private String paymentReference;
    private String webhookData;
}
