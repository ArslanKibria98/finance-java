package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.fraud.FraudCase;
import com.ksa.financing.fraud.domain.model.fraud.FraudCaseAction;
import com.ksa.financing.fraud.domain.model.fraud.FraudCaseStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudCaseRepository {

    FraudCase save(FraudCase fraudCase);

    Optional<FraudCase> findById(UUID tenantId, UUID caseId);

    List<FraudCase> findByTenantAndStatus(UUID tenantId, FraudCaseStatus status);

    List<FraudCase> findByCustomerId(UUID tenantId, String customerId);

    FraudCaseAction saveAction(FraudCaseAction action);

    List<FraudCaseAction> findActionsByCaseId(UUID tenantId, UUID caseId);

    void linkAlertToCase(UUID caseId, UUID alertId);

    String generateCaseNumber(UUID tenantId);
}
