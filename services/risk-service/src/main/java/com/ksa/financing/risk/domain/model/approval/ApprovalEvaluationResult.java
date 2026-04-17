package com.ksa.financing.risk.domain.model.approval;

import java.util.List;

public record ApprovalEvaluationResult(
    ApprovalDecision decision,
    String matchedRuleName,
    int matchedRulePriority,
    String reason,
    List<RuleEvaluationDetail> evaluationDetails
) {
    public record RuleEvaluationDetail(
        String ruleName,
        String workflowType,
        int priority,
        boolean allConditionsMet,
        List<ConditionDetail> conditionResults
    ) {}

    public record ConditionDetail(
        String field,
        String operator,
        String expectedValue,
        String actualValue,
        boolean met
    ) {}

    public static ApprovalEvaluationResult rejected(String ruleName, int priority, String reason,
                                                     List<RuleEvaluationDetail> details) {
        return new ApprovalEvaluationResult(ApprovalDecision.REJECTED, ruleName, priority, reason, details);
    }

    public static ApprovalEvaluationResult autoApproved(String ruleName, int priority,
                                                         List<RuleEvaluationDetail> details) {
        return new ApprovalEvaluationResult(ApprovalDecision.AUTO_APPROVED, ruleName, priority,
                "All auto-approval conditions met", details);
    }

    public static ApprovalEvaluationResult manualReview(String ruleName, int priority, String reason,
                                                         List<RuleEvaluationDetail> details) {
        return new ApprovalEvaluationResult(ApprovalDecision.MANUAL_REVIEW, ruleName, priority, reason, details);
    }

    public static ApprovalEvaluationResult autoApprovedDefault(List<RuleEvaluationDetail> details) {
        return new ApprovalEvaluationResult(ApprovalDecision.AUTO_APPROVED, "DEFAULT", 0,
                "No rejection or manual review rules triggered — auto approved", details);
    }
}
