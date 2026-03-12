package com.ksa.financing.risk.domain.model.credit;

import java.math.BigDecimal;

/**
 * Detail of a single criteria evaluation — one field scored against its rules.
 */
public record CriteriaEvaluationDetail(
        String fieldKey,
        String fieldName,
        String customerValue,
        boolean matched,
        BigDecimal scoredWeight,
        BigDecimal maxWeight,
        String matchedRule,
        String failureReason
) {

    public static CriteriaEvaluationDetail passed(String fieldKey, String fieldName,
                                                    String customerValue, BigDecimal weight,
                                                    BigDecimal maxWeight, String matchedRule) {
        return new CriteriaEvaluationDetail(fieldKey, fieldName, customerValue,
                true, weight, maxWeight, matchedRule, null);
    }

    public static CriteriaEvaluationDetail failed(String fieldKey, String fieldName,
                                                    String customerValue, BigDecimal maxWeight,
                                                    String failureReason) {
        return new CriteriaEvaluationDetail(fieldKey, fieldName, customerValue,
                false, BigDecimal.ZERO, maxWeight, null, failureReason);
    }

    public static CriteriaEvaluationDetail missing(String fieldKey, String fieldName,
                                                     BigDecimal maxWeight) {
        return new CriteriaEvaluationDetail(fieldKey, fieldName, null,
                false, BigDecimal.ZERO, maxWeight, null, "Field value not provided");
    }
}
