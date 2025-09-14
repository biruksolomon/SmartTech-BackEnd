package com.smarttech.service;

import com.smarttech.entity.Order;
import com.smarttech.entity.MaintenanceRequest;
import com.smarttech.entity.User;

public interface EmailService {
    void sendWelcomeEmail(User user);
    void sendEmailVerification(User user, String verificationToken);
    void sendPasswordResetEmail(User user, String resetToken);
    void sendOrderConfirmationEmail(Order order);
    void sendPaymentSuccessEmail(Order order);
    void sendMaintenanceRequestConfirmation(MaintenanceRequest request);
    void sendMaintenanceApprovalEmail(MaintenanceRequest request);
    void sendMaintenanceCompletionEmail(MaintenanceRequest request);
    void sendLowStockAlert(String productName, Integer currentStock);
    void sendMonthlyReport(String recipientEmail, byte[] reportData, String month);
}
