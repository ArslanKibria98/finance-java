package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for final risk decision after PEP/Sanctions screening.
 *
 * Evaluates overall risk based on screening results and EDD status:
 * - LOW (0-25): Auto-approve
 * - MEDIUM (26-50): Approve with flag
 * - HIGH (51-75): Hold for review
 * - CRITICAL (76-100): Block
 */
@ActivityInterface
public interface RiskDecisionActivity {

    @ActivityMethod
    RiskDecisionResult evaluateRisk(RiskDecisionInput input);

    record RiskDecisionInput(
        String customerId,
        String nationalId,
        String pepDecision,
        double pepConfidence,
        boolean sanctionsCleared,
        String eddStatus,       // null, SUBMITTED, APPROVED
        String tenantId
    ) {}

    record RiskDecisionResult(
        String riskLevel,       // LOW, MEDIUM, HIGH, CRITICAL
        int riskScore,          // 0-100
        String decision,        // APPROVE, APPROVE_FLAG, HOLD, BLOCK
        String reason
    ) {}
}
