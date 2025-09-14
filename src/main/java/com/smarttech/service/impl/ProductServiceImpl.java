package com.smarttech.service.impl;

import com.smarttech.dto.request.ProductCreateRequest;
import com.smarttech.dto.response.ProductResponse;
import com.smarttech.entity.Category;
import com.smarttech.entity.Product;
import com.smarttech.enums.ProductStatus;
import com.smarttech.exception.InsufficientStockException;
import com.smarttech.exception.ResourceNotFoundException;
import com.smarttech.mapper.ProductMapper;
import com.smarttech.repository.CategoryRepository;
import com.smarttech.repository.ProductRepository;
import com.smarttech.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating new product: {}", request.getName());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .serialNumber(request.getSerialNumber())
                .warrantyMonths(request.getWarrantyMonths())
                .stockQuantity(request.getStockQuantity())
                .minStockLevel(request.getMinStockLevel())
                .status(ProductStatus.ACTIVE)
                .imageUrls(request.getImageUrls())
                .category(category)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findEntityById(id);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAvailableProducts(pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(keyword, pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Page<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductCreateRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = findEntityById(id);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSerialNumber(request.getSerialNumber());
        product.setWarrantyMonths(request.getWarrantyMonths());
        product.setStockQuantity(request.getStockQuantity());
        product.setMinStockLevel(request.getMinStockLevel());
        product.setImageUrls(request.getImageUrls());
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", savedProduct.getId());

        return productMapper.toResponse(savedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        Product product = findEntityById(id);
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
        log.info("Product marked as discontinued with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        List<Product> products = productRepository.findLowStockProducts();
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse updateStock(Long productId, Integer quantity) {
        log.info("Updating stock for product ID: {} with quantity: {}", productId, quantity);

        Product product = findEntityById(productId);
        product.setStockQuantity(quantity);

        if (quantity <= 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        Product savedProduct = productRepository.save(product);
        log.info("Stock updated successfully for product ID: {}", productId);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Product findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductAvailable(Long productId, Integer quantity) {
        Product product = findEntityById(productId);
        return product.getStatus() == ProductStatus.ACTIVE && 
               product.getStockQuantity() >= quantity;
    }
}
