package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface FraudRuleRepository {

    PageResponse<FraudRule> findActiveByTenant(UUID tenantId, PageQuery pageQuery);

    PageResponse<FraudRule> findAllByTenant(UUID tenantId, PageQuery pageQuery);

    Optional<FraudRule> findByTenantAndRuleId(UUID tenantId, FraudRuleId ruleId);

    FraudRule save(FraudRule rule);

    void updateStatus(UUID tenantId, FraudRuleId ruleId, String status);

    void updateParameters(UUID tenantId, FraudRuleId ruleId, String parametersJson);
}
