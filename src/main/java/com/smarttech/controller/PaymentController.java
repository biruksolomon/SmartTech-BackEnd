package com.smarttech.controller;

import com.smarttech.dto.response.PaymentResponse;
import com.smarttech.service.PaymentService;
import com.yaphet.chapa.model.InitializeResponseData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Initialize payment", description = "Initialize payment for an order")
    public ResponseEntity<InitializeResponseData> initializePayment(@PathVariable Long orderId) {
        InitializeResponseData response = paymentService.initializePayment(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get order payments", description = "Get all payments for an order")
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(@PathVariable Long orderId) {
        List<PaymentResponse> payments = paymentService.getOrderPayments(orderId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/reference/{paymentReference}")
    @Operation(summary = "Get payment by reference", description = "Get payment details by reference")
    public ResponseEntity<PaymentResponse> getPaymentByReference(@PathVariable String paymentReference) {
        PaymentResponse payment = paymentService.getPaymentByReference(paymentReference);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/order/{orderId}/status")
    @Operation(summary = "Check payment status", description = "Check if order is fully paid")
    public ResponseEntity<Boolean> isOrderFullyPaid(@PathVariable Long orderId) {
        boolean isFullyPaid = paymentService.isOrderFullyPaid(orderId);
        return ResponseEntity.ok(isFullyPaid);
    }
}
