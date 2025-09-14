package com.smarttech.mapper;

import com.smarttech.dto.response.OrderItemResponse;
import com.smarttech.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {ProductMapper.class})
public interface OrderItemMapper {
    OrderItemResponse toResponse(OrderItem orderItem);
}
