package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyRuleId;
import com.ksa.financing.collections.domain.model.DelinquencyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DelinquencyRuleRepository {

    DelinquencyRule save(DelinquencyRule rule);

    Optional<DelinquencyRule> findById(UUID tenantId, DelinquencyRuleId id);

    Optional<DelinquencyRule> findActive(UUID tenantId, UUID productId, DelinquencyType type);

    List<DelinquencyRule> findAllForProduct(UUID tenantId, UUID productId);

    void softDelete(UUID tenantId, DelinquencyRuleId id);

    boolean removeEarlySettlementConfig(UUID tenantId, DelinquencyRuleId ruleId, UUID configId);
}
