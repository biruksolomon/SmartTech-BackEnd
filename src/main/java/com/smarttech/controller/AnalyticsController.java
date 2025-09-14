package com.smarttech.controller;

import com.smarttech.dto.response.DashboardStatsResponse;
import com.smarttech.dto.response.SalesReportResponse;
import com.smarttech.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
@Tag(name = "Analytics", description = "Analytics and reporting APIs (Admin only)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics", description = "Get comprehensive dashboard statistics")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = analyticsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/sales-report")
    @Operation(summary = "Get sales report", description = "Get sales report for specified date range")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        SalesReportResponse report = analyticsService.getSalesReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/tax-report/{year}/{month}")
    @Operation(summary = "Generate monthly tax report", description = "Generate Excel tax report for ERCA")
    public ResponseEntity<byte[]> generateMonthlyTaxReport(
            @PathVariable int year,
            @PathVariable int month) {
        byte[] reportData = analyticsService.generateMonthlyTaxReport(year, month);
        
        String filename = String.format("Monthly_Tax_Report_%d_%02d.xlsx", year, month);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(reportData);
    }

    @PostMapping("/send-low-stock-alerts")
    @Operation(summary = "Send low stock alerts", description = "Manually trigger low stock alert emails")
    public ResponseEntity<String> sendLowStockAlerts() {
        analyticsService.sendLowStockAlerts();
        return ResponseEntity.ok("Low stock alerts sent successfully");
    }
}
