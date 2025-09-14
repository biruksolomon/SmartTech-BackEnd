package com.smarttech.service.impl;

import com.smarttech.dto.request.PaymentRequestDTO;
import com.smarttech.dto.response.PaymentResponse;
import com.smarttech.entity.Order;
import com.smarttech.entity.Payment;
import com.smarttech.enums.OrderStatus;
import com.smarttech.enums.PaymentMethod;
import com.smarttech.enums.PaymentStatus;
import com.smarttech.exception.PaymentException;
import com.smarttech.exception.ResourceNotFoundException;
import com.smarttech.mapper.PaymentMapper;
import com.smarttech.repository.PaymentRepository;
import com.smarttech.service.ChapaService;
import com.smarttech.service.OrderService;
import com.smarttech.service.PaymentService;
import com.smarttech.service.UserService;
import com.yaphet.chapa.model.InitializeResponseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final UserService userService;
    private final ChapaService chapaService;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Override
    public InitializeResponseData initializePayment(Long orderId) {
        log.info("Initializing payment for order ID: {}", orderId);

        Order order = orderService.findEntityById(orderId);

        // Check if order is already paid
        if (isOrderFullyPaid(orderId)) {
            throw new PaymentException("Order is already fully paid");
        }

        // Generate unique payment reference
        String paymentReference = generatePaymentReference(order.getOrderNumber());

        // Create payment record
        Payment payment = Payment.builder()
                .order(order)
                .paymentReference(paymentReference)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        // Prepare Chapa payment request
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
        paymentRequest.setAmount(order.getTotalAmount());
        paymentRequest.setCurrency("ETB");
        paymentRequest.setFirstName(order.getCustomer().getFirstName());
        paymentRequest.setLastName(order.getCustomer().getLastName());
        paymentRequest.setEmail(order.getCustomer().getEmail());
        paymentRequest.setTxRef(paymentReference); // Use our generated reference
        paymentRequest.setCallbackUrl("http://localhost:" + serverPort + contextPath + "/webhooks/chapa/payment");

        try {
            InitializeResponseData response = chapaService.initializePayment(paymentRequest);


                payment.setChapaReference(paymentReference);

            paymentRepository.save(payment);

            log.info("Payment initialized successfully for order: {} with reference: {}",
                    order.getOrderNumber(), paymentReference);
            return response;

        } catch (Exception e) {
            log.error("Failed to initialize payment for order: {}", order.getOrderNumber(), e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment initialization failed: " + e.getMessage());
            paymentRepository.save(payment);
            throw new PaymentException("Failed to initialize payment", e);
        }
    }

    @Override
    public PaymentResponse processPaymentSuccess(String paymentReference, String webhookData) {
        log.info("Processing successful payment for reference: {}", paymentReference);

        Payment payment = findEntityByReference(paymentReference);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.CHAPA_BANK_TRANSFER);
        payment.setWebhookData(webhookData);

        Payment savedPayment = paymentRepository.save(payment);

        // Update order status
        if (isOrderFullyPaid(payment.getOrder().getId())) {
            orderService.updateOrderStatus(payment.getOrder().getId(), OrderStatus.CONFIRMED);

            // Update customer tier based on total purchases
            updateCustomerTier(payment.getOrder().getCustomer().getId());
        }

        log.info("Payment processed successfully for reference: {}", paymentReference);
        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    public PaymentResponse processPaymentFailure(String paymentReference, String reason) {
        log.info("Processing failed payment for reference: {}", paymentReference);

        Payment payment = findEntityByReference(paymentReference);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);

        Payment savedPayment = paymentRepository.save(payment);

        // Update order status
        orderService.updateOrderStatus(payment.getOrder().getId(), OrderStatus.PAYMENT_FAILED);

        log.info("Payment failure processed for reference: {}", paymentReference);
        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getOrderPayments(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        Payment payment = findEntityByReference(paymentReference);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOrderFullyPaid(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderService.findEntityById(orderId);
        return totalPaid.compareTo(order.getTotalAmount()) >= 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findEntityByReference(String paymentReference) {
        return paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + paymentReference));
    }

    /**
     * Generate a unique payment reference
     * Format: ST_PAY_YYYYMMDD_ORDERNUM_RANDOM
     */
    private String generatePaymentReference(String orderNumber) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999));
        return String.format("ST_PAY_%s_%s_%s", dateStr, orderNumber, randomStr);
    }

    private void updateCustomerTier(Long customerId) {
        try {
            // Calculate total successful payments for customer
            // This would be implemented based on your business logic
            userService.updateCustomerTier(customerId, BigDecimal.ZERO); // Placeholder
        } catch (Exception e) {
            log.error("Failed to update customer tier for customer ID: {}", customerId, e);
        }
    }
}
