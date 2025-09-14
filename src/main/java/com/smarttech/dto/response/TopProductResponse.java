package com.smarttech.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopProductResponse {
    private Long productId;
    private String productName;
    private Long totalSold;
    private BigDecimal totalRevenue;
}
