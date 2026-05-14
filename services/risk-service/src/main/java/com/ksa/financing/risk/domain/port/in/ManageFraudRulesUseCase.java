package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.FraudRule;

import java.util.UUID;

public interface ManageFraudRulesUseCase {

    PageResponse<FraudRule> listAll(UUID tenantId, PageQuery query);
    PageResponse<FraudRule> listByCategory(UUID tenantId, String category, PageQuery query);
    FraudRule getById(UUID tenantId, UUID id);
    FraudRule getByRuleId(UUID tenantId, String ruleId);
    FraudRule create(UUID tenantId, FraudRule rule);
    FraudRule update(UUID tenantId, UUID id, FraudRule updates);
    void assignBlockCode(UUID tenantId, UUID id, UUID blockCodeId);
    void delete(UUID tenantId, UUID id);
}
