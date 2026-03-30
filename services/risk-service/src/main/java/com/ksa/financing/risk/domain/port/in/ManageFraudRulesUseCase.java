package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.FraudRule;
import java.util.List;
import java.util.UUID;

public interface ManageFraudRulesUseCase {

    List<FraudRule> listAll(UUID tenantId);
    List<FraudRule> listByCategory(UUID tenantId, String category);
    FraudRule getById(UUID tenantId, UUID id);
    FraudRule getByRuleId(UUID tenantId, String ruleId);
    FraudRule create(UUID tenantId, FraudRule rule);
    FraudRule update(UUID tenantId, UUID id, FraudRule updates);
    void delete(UUID tenantId, UUID id);
}
