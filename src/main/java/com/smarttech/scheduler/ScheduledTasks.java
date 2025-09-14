package com.smarttech.scheduler;

import com.smarttech.service.AnalyticsService;
import com.smarttech.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final AnalyticsService analyticsService;
    private final EmailService emailService;

    @Value("${business.email}")
    private String adminEmail;

    @Scheduled(cron = "0 0 9 * * MON-FRI") // Every weekday at 9 AM
    public void sendLowStockAlerts() {
        log.info("Running scheduled low stock alerts check");
        try {
            analyticsService.sendLowStockAlerts();
        } catch (Exception e) {
            log.error("Error in scheduled low stock alerts", e);
        }
    }

    @Scheduled(cron = "0 0 8 1 * *") // First day of every month at 8 AM
    public void generateMonthlyTaxReport() {
        log.info("Running scheduled monthly tax report generation");
        try {
            LocalDateTime now = LocalDateTime.now().minusMonths(1);
            int year = now.getYear();
            int month = now.getMonthValue();
            
            byte[] reportData = analyticsService.generateMonthlyTaxReport(year, month);
            String monthName = now.getMonth().toString();
            
            emailService.sendMonthlyReport(adminEmail, reportData, monthName + " " + year);
            log.info("Monthly tax report sent successfully for {}/{}", month, year);
        } catch (Exception e) {
            log.error("Error in scheduled monthly tax report generation", e);
        }
    }
}
