package com.smarttech.service;

import com.smarttech.dto.response.PaymentResponse;
import com.smarttech.entity.Payment;
import com.yaphet.chapa.model.InitializeResponseData;

import java.util.List;

public interface PaymentService {
    InitializeResponseData initializePayment(Long orderId);
    PaymentResponse processPaymentSuccess(String paymentReference, String webhookData);
    PaymentResponse processPaymentFailure(String paymentReference, String reason);
    List<PaymentResponse> getOrderPayments(Long orderId);
    PaymentResponse getPaymentByReference(String paymentReference);
    boolean isOrderFullyPaid(Long orderId);
    Payment findEntityByReference(String paymentReference);
}
