package com.ksa.financing.risk.domain.model.credit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cached latest general-credit-scoring summary per customer.
 * One row per customer in customer_credit_score_current.
 */
public record CustomerCreditScoreCurrent(
        UUID customerId,
        UUID tenantId,
        BigDecimal scorePercentage,
        BigDecimal totalScore,
        BigDecimal maxPossibleScore,
        String decision,
        String reasonCode,
        String snapshotStage,
        UUID lastSnapshotId,
        OffsetDateTime updatedAt
) {}
