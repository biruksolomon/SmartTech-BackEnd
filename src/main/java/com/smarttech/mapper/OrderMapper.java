package com.smarttech.mapper;

import com.smarttech.dto.response.OrderResponse;
import com.smarttech.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {UserMapper.class, OrderItemMapper.class, PaymentMapper.class})
public interface OrderMapper {
    OrderResponse toResponse(Order order);
}
