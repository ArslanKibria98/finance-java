package com.ksa.financing.fraud.domain.port.in;

import java.util.UUID;

public interface CheckFraudHistoryUseCase {

    FraudHistoryResult checkHistory(UUID tenantId, String nationalIdHash, String mobileHash, String customerId);

    record FraudHistoryResult(
            boolean hasFraudHistory,
            int confirmedFraudCount,
            int suspectedFraudCount,
            int totalEvaluations,
            String riskLevel,
            String recommendation
    ) {}
}
