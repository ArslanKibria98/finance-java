package com.ksa.financing.collections.adapter.rest.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PenaltyWaiverResponse(
        UUID id,
        UUID tenantId,
        UUID loanId,
        UUID installmentId,
        BigDecimal originalPenalty,
        BigDecimal waivedAmount,
        BigDecimal remainingPenalty,
        String waiverType,
        String reason,
        String approvalReference,
        UUID waivedBy,
        LocalDateTime waivedAt
) {}
