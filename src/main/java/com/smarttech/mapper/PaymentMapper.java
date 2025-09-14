package com.smarttech.mapper;

import com.smarttech.dto.response.PaymentResponse;
import com.smarttech.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}
