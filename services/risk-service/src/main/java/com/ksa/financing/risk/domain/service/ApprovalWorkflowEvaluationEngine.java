package com.ksa.financing.risk.domain.service;

import com.ksa.financing.risk.domain.model.approval.ApprovalEvaluationResult;
import com.ksa.financing.risk.domain.model.approval.ApprovalEvaluationResult.ConditionDetail;
import com.ksa.financing.risk.domain.model.approval.ApprovalEvaluationResult.RuleEvaluationDetail;
import com.ksa.financing.risk.domain.model.approval.ApprovalWorkflowRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure domain service — evaluates approval workflow rules against customer data.
 * Rules are evaluated in priority order:
 * 1. REJECTION_SCENARIO rules first — if ANY matches, REJECT immediately
 * 2. AUTO_APPROVAL rules next — if ANY matches, AUTO_APPROVE
 * 3. MANUAL_APPROVAL rules last — if ANY matches, MANUAL_REVIEW
 * 4. If nothing matches — AUTO_APPROVE by default
 *
 * Within each workflow rule, ALL conditions must be met (AND logic).
 */
public class ApprovalWorkflowEvaluationEngine {

    public ApprovalEvaluationResult evaluate(List<ApprovalWorkflowRule> rules,
                                              Map<String, String> applicationData) {
        if (rules == null || rules.isEmpty()) {
            return ApprovalEvaluationResult.autoApprovedDefault(List.of());
        }

        var rejections = new ArrayList<ApprovalWorkflowRule>();
        var autoApprovals = new ArrayList<ApprovalWorkflowRule>();
        var manualReviews = new ArrayList<ApprovalWorkflowRule>();

        for (var rule : rules) {
            if (!rule.active()) continue;
            switch (rule.workflowType().toUpperCase()) {
                case "REJECTION_SCENARIO" -> rejections.add(rule);
                case "AUTO_APPROVAL" -> autoApprovals.add(rule);
                case "MANUAL_APPROVAL" -> manualReviews.add(rule);
            }
        }

        var allDetails = new ArrayList<RuleEvaluationDetail>();

        // Phase 1: Check rejection rules — first match = REJECT
        for (var rule : rejections) {
            if (rule.conditions().isEmpty()) continue;
            var detail = evaluateRule(rule, applicationData);
            allDetails.add(detail);
            if (detail.allConditionsMet()) {
                return ApprovalEvaluationResult.rejected(
                        rule.nameEn(), rule.priority(),
                        "Rejection rule matched: " + rule.nameEn(), allDetails);
            }
        }

        // Phase 2: Check auto-approval rules — first match = AUTO_APPROVE
        for (var rule : autoApprovals) {
            if (rule.conditions().isEmpty()) continue;
            var detail = evaluateRule(rule, applicationData);
            allDetails.add(detail);
            if (detail.allConditionsMet()) {
                return ApprovalEvaluationResult.autoApproved(
                        rule.nameEn(), rule.priority(), allDetails);
            }
        }

        // Phase 3: Check manual review rules — first match = MANUAL_REVIEW
        for (var rule : manualReviews) {
            if (rule.conditions().isEmpty()) continue;
            var detail = evaluateRule(rule, applicationData);
            allDetails.add(detail);
            if (detail.allConditionsMet()) {
                return ApprovalEvaluationResult.manualReview(
                        rule.nameEn(), rule.priority(),
                        "Manual review required: " + rule.nameEn(), allDetails);
            }
        }

        // Phase 4: Nothing matched — auto approve by default
        return ApprovalEvaluationResult.autoApprovedDefault(allDetails);
    }

    private RuleEvaluationDetail evaluateRule(ApprovalWorkflowRule rule,
                                               Map<String, String> applicationData) {
        var conditionResults = new ArrayList<ConditionDetail>();
        boolean allMet = true;

        for (var condition : rule.conditions()) {
            String actual = applicationData.getOrDefault(condition.field(), null);
            boolean met = evaluateCondition(condition.operator(), condition.value(), actual);
            if (!met) allMet = false;

            conditionResults.add(new ConditionDetail(
                    condition.field(), condition.operator(),
                    condition.value(), actual != null ? actual : "N/A", met));
        }

        return new RuleEvaluationDetail(
                rule.nameEn(), rule.workflowType(), rule.priority(),
                allMet, conditionResults);
    }

    private boolean evaluateCondition(String operator, String expected, String actual) {
        if (actual == null || actual.isBlank()) return false;

        return switch (operator.trim()) {
            case "=", "==" -> actual.equalsIgnoreCase(expected);
            case "!=", "<>" -> !actual.equalsIgnoreCase(expected);
            case ">", "GT" -> compareNumeric(actual, expected) > 0;
            case ">=", "GTE" -> compareNumeric(actual, expected) >= 0;
            case "<", "LT" -> compareNumeric(actual, expected) < 0;
            case "<=", "LTE" -> compareNumeric(actual, expected) <= 0;
            default -> false;
        };
    }

    private int compareNumeric(String actual, String expected) {
        try {
            return new BigDecimal(actual.trim()).compareTo(new BigDecimal(expected.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
