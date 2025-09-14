    package com.smarttech.controller;

    import com.smarttech.dto.request.ProductCreateRequest;
    import com.smarttech.dto.response.ProductResponse;
    import com.smarttech.service.ProductService;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.tags.Tag;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.web.bind.annotation.*;

    import java.math.BigDecimal;
    import java.util.List;

    @RestController
    @RequestMapping("/products")
    @RequiredArgsConstructor
    @Tag(name = "Products", description = "Product management APIs")
    public class ProductController {

        private final ProductService productService;

        @GetMapping
        @Operation(summary = "Get all products", description = "Retrieve paginated list of all products")
        public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
            Page<ProductResponse> products = productService.getAllProducts(pageable);
            return ResponseEntity.ok(products);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get product by ID", description = "Retrieve product details by ID")
        public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        }

        @GetMapping("/category/{categoryId}")
        @Operation(summary = "Get products by category", description = "Retrieve products by category ID")
        public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
                @PathVariable Long categoryId, Pageable pageable) {
            Page<ProductResponse> products = productService.getProductsByCategory(categoryId, pageable);
            return ResponseEntity.ok(products);
        }

        @GetMapping("/search")
        @Operation(summary = "Search products", description = "Search products by keyword")
        public ResponseEntity<Page<ProductResponse>> searchProducts(
                @RequestParam String keyword, Pageable pageable) {
            Page<ProductResponse> products = productService.searchProducts(keyword, pageable);
            return ResponseEntity.ok(products);
        }

        @GetMapping("/price-range")
        @Operation(summary = "Get products by price range", description = "Filter products by price range")
        public ResponseEntity<Page<ProductResponse>> getProductsByPriceRange(
                @RequestParam BigDecimal minPrice,
                @RequestParam BigDecimal maxPrice,
                Pageable pageable) {
            Page<ProductResponse> products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
            return ResponseEntity.ok(products);
        }

        @PostMapping
        @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        @Operation(summary = "Create product", description = "Create new product (Admin only)")
        public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
            ProductResponse product = productService.createProduct(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        @Operation(summary = "Update product", description = "Update existing product (Admin only)")
        public ResponseEntity<ProductResponse> updateProduct(
                @PathVariable Long id, @Valid @RequestBody ProductCreateRequest request) {
            ProductResponse product = productService.updateProduct(id, request);
            return ResponseEntity.ok(product);
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        @Operation(summary = "Delete product", description = "Delete product (Admin only)")
        public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/low-stock")
        @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        @Operation(summary = "Get low stock products", description = "Get products with low stock levels (Admin only)")
        public ResponseEntity<List<ProductResponse>> getLowStockProducts() {
            List<ProductResponse> products = productService.getLowStockProducts();
            return ResponseEntity.ok(products);
        }

        @PutMapping("/{id}/stock")
        @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        @Operation(summary = "Update product stock", description = "Update product stock quantity (Admin only)")
        public ResponseEntity<ProductResponse> updateStock(
                @PathVariable Long id, @RequestParam Integer quantity) {
            ProductResponse product = productService.updateStock(id, quantity);
            return ResponseEntity.ok(product);
        }
    }
