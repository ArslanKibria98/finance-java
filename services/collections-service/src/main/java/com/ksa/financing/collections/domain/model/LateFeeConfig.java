package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Late-fee configuration. Per Sharia, the late-fee amount is NOT revenue —
 * it is swept to a charity fund account.
 */
public record LateFeeConfig(
        boolean enabled,
        LateFeeType type,
        BigDecimal flatAmount,
        BigDecimal percentage,
        int minDpd,
        BigDecimal maxAmount,
        String charityFundAccount
) {

    public LateFeeConfig {
        if (enabled) {
            if (type == null)
                throw new IllegalArgumentException("Late-fee type required when enabled");
            if (type == LateFeeType.FLAT && (flatAmount == null || flatAmount.signum() <= 0))
                throw new IllegalArgumentException("flatAmount must be positive for FLAT type");
            if (type == LateFeeType.PERCENTAGE && (percentage == null || percentage.signum() <= 0))
                throw new IllegalArgumentException("percentage must be positive for PERCENTAGE type");
            if (minDpd < 1)
                throw new IllegalArgumentException("minDpd must be >= 1");
        }
    }

    /**
     * Compute the late fee for an installment given its outstanding amount and DPD.
     * Returns ZERO if disabled or DPD below threshold.
     */
    public BigDecimal computeFee(BigDecimal outstanding, int dpd) {
        if (!enabled || dpd < minDpd || outstanding == null || outstanding.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal raw = switch (type) {
            case FLAT -> flatAmount;
            case PERCENTAGE -> outstanding
                    .multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        };
        if (maxAmount != null && raw.compareTo(maxAmount) > 0) {
            return maxAmount;
        }
        return raw.setScale(6, RoundingMode.HALF_UP);
    }

    public static LateFeeConfig disabled() {
        return new LateFeeConfig(false, null, null, null, 1, null, null);
    }
}
