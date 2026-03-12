package com.ksa.financing.fraud.application.usecase;

import com.ksa.financing.fraud.domain.model.rule.FraudRule;
import com.ksa.financing.fraud.domain.model.rule.FraudRuleId;
import com.ksa.financing.fraud.domain.port.in.ManageFraudRulesUseCase;
import com.ksa.financing.fraud.domain.port.out.FraudRuleRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageFraudRulesService implements ManageFraudRulesUseCase {

    private final FraudRuleRepository fraudRuleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getAllRules(UUID tenantId) {
        return fraudRuleRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getActiveRules(UUID tenantId) {
        return fraudRuleRepository.findActiveByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public FraudRule getRule(UUID tenantId, FraudRuleId ruleId) {
        return fraudRuleRepository.findByTenantAndRuleId(tenantId, ruleId)
                .orElseThrow(() -> NotFoundException.forEntity("FraudRule", ruleId.name()));
    }

    @Override
    @Transactional
    public void enableRule(UUID tenantId, FraudRuleId ruleId) {
        log.info("Enabling fraud rule ruleId={} tenant={}", ruleId, tenantId);
        fraudRuleRepository.updateStatus(tenantId, ruleId, "ACTIVE");
    }

    @Override
    @Transactional
    public void disableRule(UUID tenantId, FraudRuleId ruleId) {
        log.info("Disabling fraud rule ruleId={} tenant={}", ruleId, tenantId);
        fraudRuleRepository.updateStatus(tenantId, ruleId, "DISABLED");
    }

    @Override
    @Transactional
    public void updateRuleParameters(UUID tenantId, FraudRuleId ruleId, String parametersJson) {
        log.info("Updating fraud rule parameters ruleId={} tenant={}", ruleId, tenantId);
        fraudRuleRepository.updateParameters(tenantId, ruleId, parametersJson);
    }
}
