package com.ksa.financing.product.domain.model;

import java.util.UUID;

public record DurationSettings(
    UUID id,
    int requestDurationDays,
    int approvalDurationDays,
    int disbursementDurationDays,
    int repaymentDurationDays
) {}
