package com.ksa.financing.collections.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ApprovePenaltyWaiverUseCase {
    /**
     * Approves a penalty waiver request.
     * @param approvedAmount nullable — when null, full requestedAmount is waived.
     *                       Must be {@code > 0} and {@code <= requestedAmount}.
     */
    void approve(UUID tenantId, UUID requestId, BigDecimal approvedAmount, UUID adminId);
    void reject(UUID tenantId, UUID requestId, String reason, UUID adminId);
}
