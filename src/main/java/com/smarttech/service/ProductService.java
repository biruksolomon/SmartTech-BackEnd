package com.smarttech.service;

import com.smarttech.dto.request.ProductCreateRequest;
import com.smarttech.dto.response.ProductResponse;
import com.smarttech.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request);
    ProductResponse getProductById(Long id);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);
    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);
    Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    ProductResponse updateProduct(Long id, ProductCreateRequest request);
    void deleteProduct(Long id);
    List<ProductResponse> getLowStockProducts();
    ProductResponse updateStock(Long productId, Integer quantity);
    Product findEntityById(Long id);
    boolean isProductAvailable(Long productId, Integer quantity);
}
