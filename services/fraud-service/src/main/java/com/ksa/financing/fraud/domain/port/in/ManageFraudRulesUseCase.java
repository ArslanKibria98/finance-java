package com.ksa.financing.fraud.domain.port.in;

import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.UUID;

public interface ManageFraudRulesUseCase {

    PageResponse<FraudRule> getAllRules(UUID tenantId, PageQuery pageQuery);

    PageResponse<FraudRule> getActiveRules(UUID tenantId, PageQuery pageQuery);

    FraudRule getRule(UUID tenantId, FraudRuleId ruleId);

    void enableRule(UUID tenantId, FraudRuleId ruleId);

    void disableRule(UUID tenantId, FraudRuleId ruleId);

    void updateRuleParameters(UUID tenantId, FraudRuleId ruleId, String parametersJson);
}
