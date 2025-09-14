package com.smarttech.service.impl;

import com.smarttech.entity.MaintenanceRequest;
import com.smarttech.entity.Order;
import com.smarttech.entity.User;
import com.smarttech.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${business.name}")
    private String businessName;

    @Value("${business.email}")
    private String fromEmail;

    @Override
    public void sendWelcomeEmail(User user) {
        log.info("Sending welcome email to: {}", user.getEmail());

        try {
            Context context = new Context();
            context.setVariable("customerName", user.getFirstName() + " " + user.getLastName());
            context.setVariable("businessName", businessName);
            context.setVariable("customerTier", user.getCustomerTier().toString());

            String htmlContent = generateSimpleHtmlTemplate("Welcome to " + businessName,
                    "Dear " + user.getFirstName() + ",<br><br>" +
                            "Welcome to " + businessName + "! Your account has been created successfully.<br>" +
                            "Your current tier: " + user.getCustomerTier() + "<br><br>" +
                            "Thank you for choosing us for your computer and maintenance needs.<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(user.getEmail(), "Welcome to " + businessName, htmlContent);
            log.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }

    @Override
    public void sendEmailVerification(User user, String verificationCode) {
        log.info("Sending email verification code to: {}", user.getEmail());

        try {
            String htmlContent = generateSimpleHtmlTemplate("Email Verification Code",
                    "Dear " + user.getFirstName() + ",<br><br>" +
                            "Your email verification code is: <strong style='font-size: 24px; color: #007bff;'>" + verificationCode + "</strong><br><br>" +
                            "This code will expire in 15 minutes. Please enter this code in the verification form to complete your email verification.<br><br>" +
                            "If you didn't create this account, please ignore this email.<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(user.getEmail(), "Email Verification Code - " + businessName, htmlContent);
            log.info("Email verification code sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email verification code to: {}", user.getEmail(), e);
        }
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        log.info("Sending password reset email to: {}", user.getEmail());

        try {
            String resetUrl = "http://localhost:3000/reset-password?token=" + resetToken;

            String htmlContent = generateSimpleHtmlTemplate("Password Reset Request",
                    "Dear " + user.getFirstName() + ",<br><br>" +
                            "You requested a password reset. Click the link below to reset your password:<br><br>" +
                            "<a href='" + resetUrl + "' style='background-color: #dc3545; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Reset Password</a><br><br>" +
                            "This link will expire in 1 hour. If you didn't request this, please ignore this email.<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(user.getEmail(), "Password Reset - " + businessName, htmlContent);
            log.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }

    @Override
    public void sendOrderConfirmationEmail(Order order) {
        log.info("Sending order confirmation email for order: {}", order.getOrderNumber());

        try {
            StringBuilder itemsHtml = new StringBuilder();
            order.getOrderItems().forEach(item -> {
                itemsHtml.append("<tr>")
                        .append("<td>").append(item.getProduct().getName()).append("</td>")
                        .append("<td>").append(item.getQuantity()).append("</td>")
                        .append("<td>ETB ").append(item.getUnitPrice()).append("</td>")
                        .append("<td>ETB ").append(item.getTotalPrice()).append("</td>")
                        .append("</tr>");
            });

            String htmlContent = generateSimpleHtmlTemplate("Order Confirmation - " + order.getOrderNumber(),
                    "Dear " + order.getCustomer().getFirstName() + ",<br><br>" +
                            "Your order has been confirmed!<br><br>" +
                            "<strong>Order Details:</strong><br>" +
                            "Order Number: " + order.getOrderNumber() + "<br>" +
                            "Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "<br><br>" +
                            "<table border='1' style='border-collapse: collapse; width: 100%;'>" +
                            "<tr><th>Product</th><th>Quantity</th><th>Unit Price</th><th>Total</th></tr>" +
                            itemsHtml.toString() +
                            "</table><br>" +
                            "<strong>Subtotal: ETB " + order.getSubtotal() + "</strong><br>" +
                            "<strong>VAT (15%): ETB " + order.getVatAmount() + "</strong><br>" +
                            "<strong>Total Amount: ETB " + order.getTotalAmount() + "</strong><br><br>" +
                            "Thank you for your business!<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(order.getCustomer().getEmail(), "Order Confirmation - " + order.getOrderNumber(), htmlContent);
            log.info("Order confirmation email sent successfully for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order: {}", order.getOrderNumber(), e);
        }
    }

    @Override
    public void sendPaymentSuccessEmail(Order order) {
        log.info("Sending payment success email for order: {}", order.getOrderNumber());

        try {
            String htmlContent = generateSimpleHtmlTemplate("Payment Successful - " + order.getOrderNumber(),
                    "Dear " + order.getCustomer().getFirstName() + ",<br><br>" +
                            "Your payment has been processed successfully!<br><br>" +
                            "<strong>Payment Details:</strong><br>" +
                            "Order Number: " + order.getOrderNumber() + "<br>" +
                            "Amount Paid: ETB " + order.getTotalAmount() + "<br>" +
                            "Payment Date: " + order.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "<br><br>" +
                            "Your order is now being processed and will be shipped soon.<br><br>" +
                            "Thank you for your payment!<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(order.getCustomer().getEmail(), "Payment Successful - " + order.getOrderNumber(), htmlContent);
            log.info("Payment success email sent successfully for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send payment success email for order: {}", order.getOrderNumber(), e);
        }
    }

    @Override
    public void sendMaintenanceRequestConfirmation(MaintenanceRequest request) {
        log.info("Sending maintenance request confirmation for: {}", request.getRequestNumber());

        try {
            String htmlContent = generateSimpleHtmlTemplate("Maintenance Request Received - " + request.getRequestNumber(),
                    "Dear " + request.getCustomer().getFirstName() + ",<br><br>" +
                            "We have received your maintenance request.<br><br>" +
                            "<strong>Request Details:</strong><br>" +
                            "Request Number: " + request.getRequestNumber() + "<br>" +
                            "Device Type: " + request.getDeviceType() + "<br>" +
                            "Device Model: " + (request.getDeviceModel() != null ? request.getDeviceModel() : "N/A") + "<br>" +
                            "Issue Description: " + request.getIssueDescription() + "<br>" +
                            "Request Date: " + request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "<br><br>" +
                            "Our technical team will review your request and contact you within 24 hours.<br><br>" +
                            "Thank you for choosing our maintenance services!<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(request.getCustomer().getEmail(), "Maintenance Request Received - " + request.getRequestNumber(), htmlContent);
            log.info("Maintenance request confirmation sent successfully for: {}", request.getRequestNumber());
        } catch (Exception e) {
            log.error("Failed to send maintenance request confirmation for: {}", request.getRequestNumber(), e);
        }
    }

    @Override
    public void sendMaintenanceApprovalEmail(MaintenanceRequest request) {
        log.info("Sending maintenance approval email for: {}", request.getRequestNumber());

        try {
            String costInfo = request.getIsWarrantyCovered() ?
                    "This service is covered under warranty - No charge" :
                    "Estimated Cost: ETB " + request.getEstimatedCost();

            String htmlContent = generateSimpleHtmlTemplate("Maintenance Request Approved - " + request.getRequestNumber(),
                    "Dear " + request.getCustomer().getFirstName() + ",<br><br>" +
                            "Great news! Your maintenance request has been approved.<br><br>" +
                            "<strong>Service Details:</strong><br>" +
                            "Request Number: " + request.getRequestNumber() + "<br>" +
                            "Device: " + request.getDeviceType() + "<br>" +
                            costInfo + "<br>" +
                            "Estimated Completion: " + request.getEstimatedCompletionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "<br><br>" +
                            "<strong>Next Steps:</strong><br>" +
                            "Please bring your device to our service center at your earliest convenience.<br>" +
                            "A maintenance ticket will be generated for tracking purposes.<br><br>" +
                            "Thank you for your patience!<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(request.getCustomer().getEmail(), "Maintenance Approved - " + request.getRequestNumber(), htmlContent);
            log.info("Maintenance approval email sent successfully for: {}", request.getRequestNumber());
        } catch (Exception e) {
            log.error("Failed to send maintenance approval email for: {}", request.getRequestNumber(), e);
        }
    }

    @Override
    public void sendMaintenanceCompletionEmail(MaintenanceRequest request) {
        log.info("Sending maintenance completion email for: {}", request.getRequestNumber());

        try {
            String htmlContent = generateSimpleHtmlTemplate("Maintenance Completed - " + request.getRequestNumber(),
                    "Dear " + request.getCustomer().getFirstName() + ",<br><br>" +
                            "Your device maintenance has been completed successfully!<br><br>" +
                            "<strong>Service Summary:</strong><br>" +
                            "Request Number: " + request.getRequestNumber() + "<br>" +
                            "Device: " + request.getDeviceType() + "<br>" +
                            "Completion Date: " + request.getCompletedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "<br>" +
                            "Final Cost: ETB " + (request.getActualCost() != null ? request.getActualCost() : request.getEstimatedCost()) + "<br><br>" +
                            "Your device is ready for pickup at our service center.<br>" +
                            "Please bring your maintenance ticket for verification.<br><br>" +
                            "Thank you for choosing our services!<br><br>" +
                            "Best regards,<br>" + businessName);

            sendHtmlEmail(request.getCustomer().getEmail(), "Maintenance Completed - " + request.getRequestNumber(), htmlContent);
            log.info("Maintenance completion email sent successfully for: {}", request.getRequestNumber());
        } catch (Exception e) {
            log.error("Failed to send maintenance completion email for: {}", request.getRequestNumber(), e);
        }
    }

    @Override
    public void sendLowStockAlert(String productName, Integer currentStock) {
        log.info("Sending low stock alert for product: {}", productName);

        try {
            String htmlContent = generateSimpleHtmlTemplate("Low Stock Alert - " + productName,
                    "Dear Admin,<br><br>" +
                            "This is an automated alert for low stock levels.<br><br>" +
                            "<strong>Product Details:</strong><br>" +
                            "Product Name: " + productName + "<br>" +
                            "Current Stock: " + currentStock + " units<br>" +
                            "Alert Date: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(java.time.LocalDateTime.now()) + "<br><br>" +
                            "Please restock this item to avoid stockouts.<br><br>" +
                            "Best regards,<br>Smart Tech System");

            sendHtmlEmail(fromEmail, "Low Stock Alert - " + productName, htmlContent);
            log.info("Low stock alert sent successfully for product: {}", productName);
        } catch (Exception e) {
            log.error("Failed to send low stock alert for product: {}", productName, e);
        }
    }

    @Override
    public void sendMonthlyReport(String recipientEmail, byte[] reportData, String month) {
        log.info("Sending monthly report for month: {} to: {}", month, recipientEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Monthly Tax Report - " + month + " - " + businessName);

            String htmlContent = generateSimpleHtmlTemplate("Monthly Tax Report - " + month,
                    "Dear Admin,<br><br>" +
                            "Please find attached the monthly tax report for " + month + ".<br><br>" +
                            "This report contains all successful transactions and VAT calculations for ERCA submission.<br><br>" +
                            "Best regards,<br>" + businessName);

            helper.setText(htmlContent, true);
            helper.addAttachment("Monthly_Tax_Report_" + month + ".xlsx", new ByteArrayResource(reportData));

            mailSender.send(message);
            log.info("Monthly report sent successfully for month: {} to: {}", month, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send monthly report for month: {} to: {}", month, recipientEmail, e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String generateSimpleHtmlTemplate(String title, String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<title>" + title + "</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background-color: #007bff; color: white; padding: 20px; text-align: center; }" +
                ".content { padding: 20px; background-color: #f9f9f9; }" +
                ".footer { background-color: #333; color: white; padding: 10px; text-align: center; font-size: 12px; }" +
                "table { width: 100%; border-collapse: collapse; margin: 10px 0; }" +
                "th, td { padding: 8px; text-align: left; border: 1px solid #ddd; }" +
                "th { background-color: #f2f2f2; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>" + businessName + "</h1>" +
                "</div>" +
                "<div class='content'>" +
                content +
                "</div>" +
                "<div class='footer'>" +
                "<p>&copy; 2024 " + businessName + ". All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
