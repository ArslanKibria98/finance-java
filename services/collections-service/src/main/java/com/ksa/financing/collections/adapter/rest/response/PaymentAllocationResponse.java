package com.ksa.financing.collections.adapter.rest.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentAllocationResponse(
        UUID paymentId,
        UUID installmentId,
        int allocationOrder,
        BigDecimal principalAllocated,
        BigDecimal profitAllocated,
        BigDecimal feeAllocated,
        BigDecimal totalAllocated
) {}
