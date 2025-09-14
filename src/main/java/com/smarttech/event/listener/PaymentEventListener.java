package com.smarttech.event.listener;

import com.smarttech.entity.Order;
import com.smarttech.entity.Payment;
import com.smarttech.event.PaymentFailureEvent;
import com.smarttech.event.PaymentSuccessEvent;
import com.smarttech.service.EmailService;
import com.smarttech.service.PaymentService;
import com.smarttech.service.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final EmailService emailService;
    private final PdfGenerationService pdfGenerationService;

    @EventListener
    @Async
    @Transactional // Added @Transactional to fix LazyInitializationException
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Processing payment success event for reference: {}", event.getPaymentReference());

        try {
            // Process payment success
            paymentService.processPaymentSuccess(event.getPaymentReference(), event.getWebhookData());

            // Get payment and order details with eager loading
            Payment payment = paymentService.findEntityByReference(event.getPaymentReference());
            Order order = payment.getOrder();

            order.getOrderNumber();
            order.getCustomer().getFirstName();
            order.getOrderItems().size();

            // Send confirmation email
            emailService.sendPaymentSuccessEmail(order);

            // Generate invoice PDF
            String invoicePdfUrl = pdfGenerationService.generateInvoicePdf(order);
            pdfGenerationService.createInvoiceRecord(order, invoicePdfUrl);

            log.info("Payment success event processed successfully for reference: {}", event.getPaymentReference());
        } catch (Exception e) {
            log.error("Error processing payment success event for reference: {}", event.getPaymentReference(), e);
        }
    }

    @EventListener
    @Async
    public void handlePaymentFailure(PaymentFailureEvent event) {
        log.info("Processing payment failure event for reference: {}", event.getPaymentReference());

        try {
            // Process payment failure
            paymentService.processPaymentFailure(event.getPaymentReference(), event.getReason());

            log.info("Payment failure event processed successfully for reference: {}", event.getPaymentReference());
        } catch (Exception e) {
            log.error("Error processing payment failure event for reference: {}", event.getPaymentReference(), e);
        }
    }
}
