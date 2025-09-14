package com.smarttech.service;

import com.smarttech.dto.request.OrderCreateRequest;
import com.smarttech.dto.response.OrderResponse;
import com.smarttech.entity.Order;
import com.smarttech.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderCreateRequest request, Long customerId);
    OrderResponse getOrderById(Long id);
    OrderResponse getOrderByNumber(String orderNumber);
    Page<OrderResponse> getCustomerOrders(Long customerId, Pageable pageable);
    Page<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
    List<OrderResponse> getOrdersByStatus(OrderStatus status);
    BigDecimal getTotalRevenueForPeriod(LocalDateTime startDate, LocalDateTime endDate);
    Order findEntityById(Long id);
    Order findEntityByOrderNumber(String orderNumber);
}
