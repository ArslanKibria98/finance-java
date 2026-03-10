package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Temporal activity for AML/CFT risk scoring.
 *
 * Calls the Risk Service's AML scoring endpoint to calculate a weighted risk score
 * based on the EastNets AML Risk Schema (8 categories, dominant + mutual exclusive).
 *
 * This activity is executed via HTTP call to risk-service, not locally.
 */
@ActivityInterface
public interface AmlRiskScoringActivity {

    @ActivityMethod
    AmlRiskScoringResult calculateAmlScore(AmlRiskScoringInput input);

    record AmlRiskScoringInput(
        String nationalIdHash,
        String nationality,
        String cityName,
        String occupationCode,
        BigDecimal monthlyIncome,
        String sourceOfIncome,
        String productRiskTier,
        boolean isPep,
        boolean isOnInternalList,
        String customerId,
        String tenantId,
        String idempotencyKey
    ) {}

    record AmlRiskScoringResult(
        String assessmentId,
        BigDecimal totalScore,
        String riskLevel,           // HIGH, MEDIUM, LOW
        boolean dominantOverride,
        String dominantCategory,    // PEP or INTERNAL_LIST if dominant override
        List<CategoryBreakdown> breakdown,
        boolean success,
        String errorMessage
    ) {
        /** Factory for failure responses */
        public static AmlRiskScoringResult failure(String errorMessage) {
            return new AmlRiskScoringResult(
                    null, BigDecimal.ZERO, "LOW", false, null,
                    List.of(), false, errorMessage
            );
        }
    }

    record CategoryBreakdown(
        String categoryCode,
        String categoryName,
        String matchedFactorCode,
        BigDecimal categoryWeight,
        BigDecimal factorWeightPct,
        BigDecimal computedRating
    ) {}
}
