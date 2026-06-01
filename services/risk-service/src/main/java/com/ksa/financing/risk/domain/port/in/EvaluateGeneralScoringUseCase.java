package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreCurrent;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreSnapshot;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime evaluation use case for GENERAL credit scoring during onboarding.
 * Pulls criteria from general_credit_scoring_criteria/rules (NOT product
 * criteria), runs the decision engine, optionally persists a snapshot and
 * updates the customer's current cached score.
 */
public interface EvaluateGeneralScoringUseCase {

    EligibilityEvaluationResult evaluate(EvaluateInput input);

    Optional<CustomerCreditScoreCurrent> getCurrentScore(UUID tenantId, UUID customerId);

    List<CustomerCreditScoreSnapshot> getSnapshotHistory(UUID tenantId, UUID customerId);

    record EvaluateInput(
            UUID tenantId,
            UUID customerId,
            String workflowId,
            String stage,
            Map<String, String> answers,
            boolean persistSnapshot
    ) {}
}
