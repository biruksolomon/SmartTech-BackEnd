package com.smarttech.dto.response;

import com.smarttech.enums.CustomerTier;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerTierStatsResponse {
    private CustomerTier tier;
    private Long customerCount;
    private Double percentage;
}
