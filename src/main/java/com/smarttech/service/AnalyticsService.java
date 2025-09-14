package com.smarttech.service;

import com.smarttech.dto.response.DashboardStatsResponse;
import com.smarttech.dto.response.SalesReportResponse;

import java.time.LocalDateTime;

public interface AnalyticsService {
    DashboardStatsResponse getDashboardStats();
    SalesReportResponse getSalesReport(LocalDateTime startDate, LocalDateTime endDate);
    byte[] generateMonthlyTaxReport(int year, int month);
    void sendLowStockAlerts();
}
