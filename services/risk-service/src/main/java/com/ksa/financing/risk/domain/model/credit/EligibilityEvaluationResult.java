package com.ksa.financing.risk.domain.model.credit;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of evaluating customer eligibility against a product's credit scoring rules.
 */
public record EligibilityEvaluationResult(
        boolean eligible,
        BigDecimal totalScore,
        BigDecimal maxPossibleScore,
        BigDecimal scorePercentage,
        BigDecimal minimumPassPercentage,
        int totalCriteria,
        int matchedCriteria,
        int failedCriteria,
        List<CriteriaEvaluationDetail> details,
        String summary
) {

    public static EligibilityEvaluationResult noCriteria() {
        return new EligibilityEvaluationResult(
                true, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"),
                BigDecimal.ZERO, 0, 0, 0, List.of(),
                "No scoring criteria configured for this product — default eligible"
        );
    }
}
