package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.FraudRule;

import java.util.Optional;
import java.util.UUID;

public interface FraudRuleRepository {

    PageResponse<FraudRule> findAllByTenant(UUID tenantId, PageQuery query);
    PageResponse<FraudRule> findByCategory(UUID tenantId, String category, PageQuery query);
    Optional<FraudRule> findById(UUID id);
    Optional<FraudRule> findByRuleId(UUID tenantId, String ruleId);
    FraudRule save(FraudRule rule);
    void delete(UUID id);
}
