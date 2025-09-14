package com.smarttech.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentFailureEvent {
    private String paymentReference;
    private String reason;
}
