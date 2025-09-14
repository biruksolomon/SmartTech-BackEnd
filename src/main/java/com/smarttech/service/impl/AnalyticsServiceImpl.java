package com.smarttech.service.impl;

import com.smarttech.dto.response.*;
import com.smarttech.entity.Order;
import com.smarttech.entity.Product;
import com.smarttech.entity.User;
import com.smarttech.enums.CustomerTier;
import com.smarttech.enums.MaintenanceStatus;
import com.smarttech.enums.OrderStatus;
import com.smarttech.enums.UserRole;
import com.smarttech.repository.*;
import com.smarttech.service.AnalyticsService;
import com.smarttech.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        log.info("Generating dashboard statistics");

        // Basic counts
        Long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        Long totalOrders = orderRepository.count();
        Long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        Long completedOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        Long totalProducts = productRepository.count();
        Long lowStockProducts = (long) productRepository.findLowStockProducts().size();
        Long pendingMaintenanceRequests = maintenanceRequestRepository.countByStatus(MaintenanceStatus.PENDING);
        Long completedMaintenanceRequests = maintenanceRequestRepository.countByStatus(MaintenanceStatus.COMPLETED);

        // Revenue calculations
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
        
        BigDecimal totalRevenue = orderRepository.getTotalRevenueForPeriod(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.now());
        BigDecimal monthlyRevenue = orderRepository.getTotalRevenueForPeriod(startOfMonth, endOfMonth);

        // Customer tier statistics
        List<CustomerTierStatsResponse> customerTierStats = Arrays.stream(CustomerTier.values())
                .map(tier -> {
                    Long count = userRepository.countByCustomerTier(tier);
                    Double percentage = totalCustomers > 0 ? (count.doubleValue() / totalCustomers.doubleValue()) * 100 : 0.0;
                    return CustomerTierStatsResponse.builder()
                            .tier(tier)
                            .customerCount(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());

        // Top products (placeholder - would need custom query)
        List<TopProductResponse> topProducts = List.of(); // Implement based on order items

        return DashboardStatsResponse.builder()
                .totalCustomers(totalCustomers)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .pendingMaintenanceRequests(pendingMaintenanceRequests)
                .completedMaintenanceRequests(completedMaintenanceRequests)
                .topProducts(topProducts)
                .customerTierStats(customerTierStats)
                .build();
    }

    @Override
    public SalesReportResponse getSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating sales report from {} to {}", startDate, endDate);

        List<Order> orders = orderRepository.findOrdersBetweenDates(startDate, endDate);
        
        Long totalOrders = (long) orders.size();
        BigDecimal totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalVat = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Convert orders to response DTOs (would need OrderMapper)
        List<OrderResponse> orderResponses = List.of(); // Implement mapping

        return SalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalVat(totalVat)
                .orders(orderResponses)
                .topProducts(List.of()) // Implement top products calculation
                .build();
    }

    @Override
    public byte[] generateMonthlyTaxReport(int year, int month) {
        log.info("Generating monthly tax report for {}/{}", month, year);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Order> orders = orderRepository.findOrdersBetweenDates(startDate, endDate)
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Monthly Tax Report");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Order Number", "Date", "Customer Name", "Customer Email", 
                               "Subtotal (ETB)", "VAT Amount (ETB)", "Total Amount (ETB)"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            int rowNum = 1;
            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalVat = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getCreatedAt().toString());
                row.createCell(2).setCellValue(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName());
                row.createCell(3).setCellValue(order.getCustomer().getEmail());
                row.createCell(4).setCellValue(order.getSubtotal().doubleValue());
                row.createCell(5).setCellValue(order.getVatAmount().doubleValue());
                row.createCell(6).setCellValue(order.getTotalAmount().doubleValue());

                totalSubtotal = totalSubtotal.add(order.getSubtotal());
                totalVat = totalVat.add(order.getVatAmount());
                totalAmount = totalAmount.add(order.getTotalAmount());
            }

            // Add summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            Cell summaryCell = summaryRow.createCell(0);
            summaryCell.setCellValue("TOTAL");
            summaryCell.setCellStyle(headerStyle);
            
            summaryRow.createCell(4).setCellValue(totalSubtotal.doubleValue());
            summaryRow.createCell(5).setCellValue(totalVat.doubleValue());
            summaryRow.createCell(6).setCellValue(totalAmount.doubleValue());

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error generating monthly tax report", e);
            throw new RuntimeException("Failed to generate monthly tax report", e);
        }
    }

    @Override
    @Transactional
    public void sendLowStockAlerts() {
        log.info("Checking for low stock products and sending alerts");

        List<Product> lowStockProducts = productRepository.findLowStockProducts();
        
        for (Product product : lowStockProducts) {
            emailService.sendLowStockAlert(product.getName(), product.getStockQuantity());
        }

        log.info("Sent {} low stock alerts", lowStockProducts.size());
    }
}
