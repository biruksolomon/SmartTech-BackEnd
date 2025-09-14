package com.smarttech.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SalesReportResponse {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalVat;
    private List<OrderResponse> orders;
    private List<TopProductResponse> topProducts;
}
