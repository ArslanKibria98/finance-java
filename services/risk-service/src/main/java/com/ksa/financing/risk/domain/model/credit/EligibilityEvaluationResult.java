package com.ksa.financing.risk.domain.model.credit;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of evaluating customer eligibility against a product's credit scoring rules.
 */
public record EligibilityEvaluationResult(
        boolean eligible,
        CreditDecision decision,
        BigDecimal totalScore,
        BigDecimal maxPossibleScore,
        BigDecimal scorePercentage,
        BigDecimal minimumPassPercentage,
        BigDecimal greenThreshold,
        BigDecimal amberThreshold,
        int totalCriteria,
        int matchedCriteria,
        int failedCriteria,
        List<CriteriaEvaluationDetail> details,
        String reasonCode,
        String summary
) {

    public static EligibilityEvaluationResult noCriteria() {
        return new EligibilityEvaluationResult(
                true, CreditDecision.AUTO_APPROVE,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, 0, List.of(),
                "NO_CRITERIA",
                "No scoring criteria configured for this product — default eligible"
        );
    }
}
