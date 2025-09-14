package com.smarttech.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {
    private Long totalCustomers;
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private Long totalProducts;
    private Long lowStockProducts;
    private Long pendingMaintenanceRequests;
    private Long completedMaintenanceRequests;
    private List<TopProductResponse> topProducts;
    private List<CustomerTierStatsResponse> customerTierStats;
}
