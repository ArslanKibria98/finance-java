package com.ksa.financing.risk.domain.model.credit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Point-in-time scoring snapshot captured during a customer's onboarding
 * (one per third-party stage). Persisted to customer_credit_score_snapshots.
 *
 * Stage values: NAFATH_VERIFIED, YAKEEN_VERIFIED, EDD_SUBMITTED,
 * SALARY_FETCHED, AML_SCORED, ONBOARDING_COMPLETE.
 */
public record CustomerCreditScoreSnapshot(
        UUID id,
        UUID tenantId,
        UUID customerId,
        String workflowId,
        String snapshotStage,
        BigDecimal scorePercentage,
        BigDecimal totalScore,
        BigDecimal maxPossibleScore,
        String decision,
        String reasonCode,
        int matchedCriteria,
        int totalCriteria,
        Map<String, String> inputs,
        List<CriteriaEvaluationDetail> breakdown,
        String summary,
        OffsetDateTime createdAt
) {}
