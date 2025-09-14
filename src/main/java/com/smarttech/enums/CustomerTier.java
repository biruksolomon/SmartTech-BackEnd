package com.smarttech.enums;

import java.math.BigDecimal;

public enum CustomerTier {
    BRONZE(BigDecimal.ZERO, new BigDecimal("50000")),
    SILVER(new BigDecimal("50000"), new BigDecimal("150000")),
    GOLD(new BigDecimal("150000"), new BigDecimal("500000")),
    DIAMOND(new BigDecimal("500000"), null);

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    CustomerTier(BigDecimal minAmount, BigDecimal maxAmount) {
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public static CustomerTier calculateTier(BigDecimal totalPurchases) {
        if (totalPurchases.compareTo(DIAMOND.minAmount) >= 0) {
            return DIAMOND;
        } else if (totalPurchases.compareTo(GOLD.minAmount) >= 0) {
            return GOLD;
        } else if (totalPurchases.compareTo(SILVER.minAmount) >= 0) {
            return SILVER;
        } else {
            return BRONZE;
        }
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }
}
