package com.ksa.financing.fraud.application.usecase;

import com.ksa.financing.fraud.domain.port.in.CheckFraudHistoryUseCase;
import com.ksa.financing.fraud.domain.port.out.FraudEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckFraudHistoryService implements CheckFraudHistoryUseCase {

    private final FraudEvaluationRepository fraudEvaluationRepository;

    @Override
    public FraudHistoryResult checkHistory(UUID tenantId, String nationalIdHash, String mobileHash, String customerId) {
        log.debug("Checking fraud history: tenantId={} customerId={}", tenantId, customerId);

        int confirmedFraud = 0;
        int suspectedFraud = 0;
        int totalEvaluations = 0;

        if (customerId != null) {
            var stats = fraudEvaluationRepository.getEvaluationStats(tenantId, customerId);
            confirmedFraud = stats.blockedCount();
            suspectedFraud = stats.heldCount() + stats.alertedCount();
            totalEvaluations = stats.totalCount();
        }

        var riskLevel = confirmedFraud > 0 ? "CRITICAL" : suspectedFraud > 2 ? "HIGH" : suspectedFraud > 0 ? "MEDIUM" : "LOW";
        var recommendation = confirmedFraud > 0 ? "BLOCK" : suspectedFraud > 2 ? "REVIEW_MANUAL" : "PASS";
        boolean hasFraudHistory = confirmedFraud > 0 || suspectedFraud > 0;

        log.info("Fraud history check result: customerId={} hasFraudHistory={} confirmed={} suspected={} recommendation={}",
                customerId, hasFraudHistory, confirmedFraud, suspectedFraud, recommendation);

        return new FraudHistoryResult(hasFraudHistory, confirmedFraud, suspectedFraud, totalEvaluations, riskLevel, recommendation);
    }
}
