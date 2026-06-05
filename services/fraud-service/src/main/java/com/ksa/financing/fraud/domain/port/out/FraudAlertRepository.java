package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.fraud.FraudAlert;
import com.ksa.financing.fraud.domain.model.fraud.FraudAlertStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudAlertRepository {

    FraudAlert save(FraudAlert alert);

    Optional<FraudAlert> findById(UUID tenantId, UUID alertId);

    List<FraudAlert> findByTenantAndStatus(UUID tenantId, FraudAlertStatus status);

    List<FraudAlert> findByCustomerId(UUID tenantId, String customerId);

    long countByTenantAndStatus(UUID tenantId, FraudAlertStatus status);
}
