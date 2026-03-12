package com.ksa.financing.fraud.domain.port.in;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvaluationResult;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;

import java.util.UUID;

public interface EvaluateFraudEventUseCase {

    /**
     * Evaluates a fraud event against all active tenant rules.
     * Pipeline: receive → enrich → evaluate rules → decide → persist → alert.
     */
    FraudEvaluationResult evaluate(UUID tenantId, FraudEvent event);
}
