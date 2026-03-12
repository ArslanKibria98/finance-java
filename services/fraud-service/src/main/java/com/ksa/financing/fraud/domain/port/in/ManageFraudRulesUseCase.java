package com.ksa.financing.fraud.domain.port.in;

import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;

import java.util.List;
import java.util.UUID;

public interface ManageFraudRulesUseCase {

    List<FraudRule> getAllRules(UUID tenantId);

    List<FraudRule> getActiveRules(UUID tenantId);

    FraudRule getRule(UUID tenantId, FraudRuleId ruleId);

    void enableRule(UUID tenantId, FraudRuleId ruleId);

    void disableRule(UUID tenantId, FraudRuleId ruleId);

    void updateRuleParameters(UUID tenantId, FraudRuleId ruleId, String parametersJson);
}
