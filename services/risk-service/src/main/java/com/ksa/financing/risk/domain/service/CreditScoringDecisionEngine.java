package com.ksa.financing.risk.domain.service;

import com.ksa.financing.risk.domain.model.credit.CreditDecision;
import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.model.credit.CreditScoringRule;
import com.ksa.financing.risk.domain.model.credit.CriteriaEvaluationDetail;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Pure domain service — evaluates customer eligibility answers against
 * a product's credit scoring criteria and rules.
 *
 * Zero framework imports.
 *
 * Scoring algorithm:
 * 1. For each enabled criteria, resolve the field_key from the field definition.
 * 2. Look up the customer's answer by field_key.
 * 3. Evaluate each rule (operator + value) against the customer's answer.
 * 4. The BEST matching rule (highest weight) contributes to the total score.
 * 5. scorePercentage = totalScore / maxPossibleScore * 100
 * 6. eligible = scorePercentage >= minimumPassPercentage
 */
public class CreditScoringDecisionEngine {

    /**
     * Evaluate customer answers against product scoring criteria.
     *
     * @param criteria         enabled criteria for the product (with rules loaded)
     * @param fieldDefinitions all field definitions (to resolve field_key from criteria.fieldDefinitionId)
     * @param answers          customer answers: field_key → value (e.g., "salary" → "15000")
     * @param minPassPercentage minimum score percentage to be eligible (e.g., 60)
     * @return evaluation result with score, details, and eligibility decision
     */
    public EligibilityEvaluationResult evaluate(
            List<CreditScoringCriteria> criteria,
            List<CreditScoringFieldDefinition> fieldDefinitions,
            Map<String, String> answers,
            BigDecimal minPassPercentage) {
        return evaluate(criteria, fieldDefinitions, answers,
                minPassPercentage, minPassPercentage, minPassPercentage);
    }

    /**
     * BRS-aligned overload that produces a Green/Amber/Red decision.
     *
     * @param greenThreshold score % at or above which decision is AUTO_APPROVE
     * @param amberThreshold score % at or above which decision is REFER_MANUAL_REVIEW
     *                       (below this → AUTO_REJECT)
     * @param minPassPercentage legacy {@code eligible} boolean threshold; pass the
     *                          green threshold here to keep backward-compat callers happy.
     */
    public EligibilityEvaluationResult evaluate(
            List<CreditScoringCriteria> criteria,
            List<CreditScoringFieldDefinition> fieldDefinitions,
            Map<String, String> answers,
            BigDecimal minPassPercentage,
            BigDecimal greenThreshold,
            BigDecimal amberThreshold) {

        if (criteria == null || criteria.isEmpty()) {
            return EligibilityEvaluationResult.noCriteria();
        }

        // Build field definition lookup: id → definition
        var fieldDefMap = new java.util.HashMap<java.util.UUID, CreditScoringFieldDefinition>();
        for (var fd : fieldDefinitions) {
            fieldDefMap.put(fd.id(), fd);
        }

        var details = new ArrayList<CriteriaEvaluationDetail>();
        var totalScore = BigDecimal.ZERO;
        var maxPossibleScore = BigDecimal.ZERO;
        int matchedCount = 0;
        int failedCount = 0;

        for (var criterion : criteria) {
            if (!criterion.enabled()) continue;
            if (criterion.rules() == null || criterion.rules().isEmpty()) continue;

            // Resolve field_key
            String fieldKey;
            String fieldName;
            if (criterion.custom() && criterion.customName() != null) {
                fieldKey = criterion.customName();
                fieldName = criterion.customName();
            } else {
                var fieldDef = fieldDefMap.get(criterion.fieldDefinitionId());
                if (fieldDef == null) continue;
                fieldKey = fieldDef.fieldKey();
                fieldName = fieldDef.nameEn();
            }

            // Max weight for this criteria = highest weight among its rules
            var maxWeight = criterion.rules().stream()
                    .map(CreditScoringRule::weight)
                    .filter(w -> w != null)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            maxPossibleScore = maxPossibleScore.add(maxWeight);

            // Get customer's answer
            var customerValue = answers.get(fieldKey);
            if (customerValue == null || customerValue.isBlank()) {
                details.add(CriteriaEvaluationDetail.missing(fieldKey, fieldName, maxWeight));
                failedCount++;
                continue;
            }

            // Evaluate rules — find the best matching rule (highest weight)
            CreditScoringRule bestMatch = null;
            BigDecimal bestWeight = BigDecimal.ZERO;

            for (var rule : criterion.rules()) {
                if (evaluateRule(rule, customerValue)) {
                    var ruleWeight = rule.weight() != null ? rule.weight() : BigDecimal.ZERO;
                    if (bestMatch == null || ruleWeight.compareTo(bestWeight) > 0) {
                        bestMatch = rule;
                        bestWeight = ruleWeight;
                    }
                }
            }

            if (bestMatch != null) {
                totalScore = totalScore.add(bestWeight);
                details.add(CriteriaEvaluationDetail.passed(
                        fieldKey, fieldName, customerValue, bestWeight, maxWeight,
                        bestMatch.operator() + " " + bestMatch.value()));
                matchedCount++;
            } else {
                details.add(CriteriaEvaluationDetail.failed(
                        fieldKey, fieldName, customerValue, maxWeight,
                        "No rule matched for value: " + customerValue));
                failedCount++;
            }
        }

        // Calculate score percentage
        var scorePercentage = maxPossibleScore.compareTo(BigDecimal.ZERO) > 0
                ? totalScore.multiply(new BigDecimal("100"))
                    .divide(maxPossibleScore, 2, RoundingMode.HALF_UP)
                : new BigDecimal("100");

        var eligible = scorePercentage.compareTo(minPassPercentage) >= 0;

        // BRS Green/Amber/Red mapping
        CreditDecision decision;
        String reasonCode;
        if (scorePercentage.compareTo(greenThreshold) >= 0) {
            decision = CreditDecision.AUTO_APPROVE;
            reasonCode = "GREEN_AUTO_APPROVE";
        } else if (scorePercentage.compareTo(amberThreshold) >= 0) {
            decision = CreditDecision.REFER_MANUAL_REVIEW;
            reasonCode = "AMBER_MANUAL_REVIEW";
        } else {
            decision = CreditDecision.AUTO_REJECT;
            reasonCode = "RED_AUTO_REJECT";
        }

        var summary = switch (decision) {
            case AUTO_APPROVE -> String.format(
                    "Auto-approve — score %.1f%% (green ≥ %.1f%%)", scorePercentage, greenThreshold);
            case REFER_MANUAL_REVIEW -> String.format(
                    "Manual review — score %.1f%% (amber %.1f%%-%.1f%%)",
                    scorePercentage, amberThreshold, greenThreshold);
            case AUTO_REJECT -> String.format(
                    "Auto-reject — score %.1f%% below amber threshold %.1f%%",
                    scorePercentage, amberThreshold);
        };

        return new EligibilityEvaluationResult(
                eligible, decision, totalScore, maxPossibleScore, scorePercentage,
                minPassPercentage, greenThreshold, amberThreshold,
                matchedCount + failedCount, matchedCount, failedCount,
                details, reasonCode, summary);
    }

    /**
     * Evaluate a single rule against a customer value.
     */
    private boolean evaluateRule(CreditScoringRule rule, String customerValue) {
        try {
            return switch (rule.operator()) {
                case EQ -> evaluateEq(rule.value(), customerValue);
                case GT -> evaluateNumeric(customerValue, rule.value(), 1);
                case GTE -> evaluateNumeric(customerValue, rule.value(), 0);
                case LT -> evaluateNumeric(customerValue, rule.value(), -1);
                case LTE -> evaluateNumericLte(customerValue, rule.value());
                case BETWEEN -> evaluateBetween(customerValue, rule.value());
                case IN -> evaluateIn(customerValue, rule.value());
            };
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateEq(String ruleValue, String customerValue) {
        // Try numeric comparison first, fall back to string
        try {
            return new BigDecimal(customerValue).compareTo(new BigDecimal(ruleValue)) == 0;
        } catch (NumberFormatException e) {
            return customerValue.equalsIgnoreCase(ruleValue);
        }
    }

    /**
     * @param direction 1 = GT (customer > rule), 0 = GTE (customer >= rule), -1 = LT (customer < rule)
     */
    private boolean evaluateNumeric(String customerValue, String ruleValue, int direction) {
        var cv = new BigDecimal(customerValue);
        var rv = new BigDecimal(ruleValue);
        int cmp = cv.compareTo(rv);
        return direction == 0 ? cmp >= 0 : (direction > 0 ? cmp > 0 : cmp < 0);
    }

    private boolean evaluateNumericLte(String customerValue, String ruleValue) {
        return new BigDecimal(customerValue).compareTo(new BigDecimal(ruleValue)) <= 0;
    }

    /**
     * BETWEEN format: "min,max" e.g., "21,60"
     */
    private boolean evaluateBetween(String customerValue, String ruleValue) {
        var parts = ruleValue.split(",");
        if (parts.length != 2) return false;
        var cv = new BigDecimal(customerValue);
        var min = new BigDecimal(parts[0].trim());
        var max = new BigDecimal(parts[1].trim());
        return cv.compareTo(min) >= 0 && cv.compareTo(max) <= 0;
    }

    /**
     * IN format: "val1,val2,val3" e.g., "GOVERNMENT,SEMI_GOVERNMENT,MILITARY"
     */
    private boolean evaluateIn(String customerValue, String ruleValue) {
        var values = ruleValue.split(",");
        for (var v : values) {
            if (customerValue.trim().equalsIgnoreCase(v.trim())) return true;
        }
        return false;
    }
}
