package com.smarttech.dto.response;

import com.smarttech.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String serialNumber;
    private Integer warrantyMonths;
    private Integer stockQuantity;
    private Integer minStockLevel;
    private ProductStatus status;
    private List<String> imageUrls;
    private CategoryResponse category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
