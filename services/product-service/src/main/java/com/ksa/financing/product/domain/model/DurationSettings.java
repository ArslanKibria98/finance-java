package com.ksa.financing.product.domain.model;

import java.util.UUID;

public record DurationSettings(
    UUID id,
    Integer requestDurationDays,
    Integer approvalDurationDays,
    Integer disbursementDurationHours,
    Integer repaymentDurationDays
) {

    public static DurationSettings defaults() {
        return new DurationSettings(null, 0, 0, 0, 0);
    }
}
