package com.smarttech.service.impl;

import com.smarttech.dto.request.OrderCreateRequest;
import com.smarttech.dto.response.OrderResponse;
import com.smarttech.entity.Order;
import com.smarttech.entity.OrderItem;
import com.smarttech.entity.Product;
import com.smarttech.entity.User;
import com.smarttech.enums.OrderStatus;
import com.smarttech.exception.InsufficientStockException;
import com.smarttech.exception.ResourceNotFoundException;
import com.smarttech.mapper.OrderMapper;
import com.smarttech.repository.OrderRepository;
import com.smarttech.service.OrderService;
import com.smarttech.service.ProductService;
import com.smarttech.service.UserService;
import com.smarttech.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserService userService;
    private final ProductService productService;

    @Value("${business.vat-rate}")
    private BigDecimal vatRate;

    @Override
    public OrderResponse createOrder(OrderCreateRequest request, Long customerId) {
        log.info("Creating order for customer ID: {}", customerId);

        User customer = userService.findEntityById(customerId);

        // Validate stock availability
        for (OrderCreateRequest.OrderItemRequest itemRequest : request.getItems()) {
            if (!productService.isProductAvailable(itemRequest.getProductId(), itemRequest.getQuantity())) {
                throw new InsufficientStockException("Insufficient stock for product ID: " + itemRequest.getProductId());
            }
        }

        // Create order
        Order order = Order.builder()
                .orderNumber(OrderNumberGenerator.generate())
                .customer(customer)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .build();

        // Create order items and calculate totals
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmountInclusiveVAT = BigDecimal.ZERO;

        for (OrderCreateRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productService.findEntityById(itemRequest.getProductId());

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotal)
                    .serialNumber(product.getSerialNumber())
                    .build();

            orderItems.add(orderItem);
            totalAmountInclusiveVAT = totalAmountInclusiveVAT.add(itemTotal);
        }

        // Formula: subtotal = totalAmount / (1 + vatRate)
        // Formula: vatAmount = totalAmount - subtotal
        BigDecimal divisor = BigDecimal.ONE.add(vatRate); // 1 + 0.15 = 1.15
        BigDecimal subtotal = totalAmountInclusiveVAT.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = totalAmountInclusiveVAT.subtract(subtotal);

        order.setOrderItems(orderItems);
        order.setSubtotal(subtotal);
        order.setVatAmount(vatAmount);
        order.setTotalAmount(totalAmountInclusiveVAT);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with number: {}", savedOrder.getOrderNumber());

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findEntityById(id);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = findEntityByOrderNumber(orderNumber);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(Long customerId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByCustomerId(customerId, pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        log.info("Updating order status for ID: {} to {}", orderId, status);

        Order order = findEntityById(orderId);
        order.setStatus(status);

        // Update stock when order is confirmed
        if (status == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                int newStock = product.getStockQuantity() - item.getQuantity();
                productService.updateStock(product.getId(), newStock);
            }
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order status updated successfully for ID: {}", orderId);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = orderRepository.getTotalRevenueForPeriod(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public Order findEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Order findEntityByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
    }
}
