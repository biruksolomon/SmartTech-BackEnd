package com.smarttech.mapper;

import com.smarttech.dto.response.ProductResponse;
import com.smarttech.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CategoryMapper.class})
public interface ProductMapper {
    ProductResponse toResponse(Product product);
}
