package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvaluationResult;

import java.util.Optional;
import java.util.UUID;

public interface FraudEvaluationRepository {

    FraudEvaluationResult save(FraudEvaluationResult result);

    Optional<FraudEvaluationResult> findByTenantAndEventId(UUID tenantId, String eventId);

    EvaluationStats getEvaluationStats(UUID tenantId, String customerId);

    record EvaluationStats(int totalCount, int blockedCount, int heldCount, int alertedCount) {}
}
