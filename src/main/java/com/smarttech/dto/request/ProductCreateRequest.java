package com.smarttech.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductCreateRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Product description is required")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    private String serialNumber;

    @Min(value = 0, message = "Warranty months cannot be negative")
    @Max(value = 120, message = "Warranty cannot exceed 120 months")
    private Integer warrantyMonths;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 0, message = "Minimum stock level cannot be negative")
    private Integer minStockLevel = 5;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private List<String> imageUrls;
}
