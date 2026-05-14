package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;
import com.ksa.financing.risk.domain.port.in.ManageScenarioRuleUseCase;
import com.ksa.financing.risk.domain.port.out.ScenarioRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageScenarioRuleService implements ManageScenarioRuleUseCase {

    private final ScenarioRuleRepository scenarioRuleRepository;

    @Override
    @Transactional
    public ScenarioRule create(UUID tenantId, CreateScenarioRuleCommand command) {
        var rule = new ScenarioRule();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(tenantId);
        rule.setScenarioName(command.scenarioName());
        rule.setScenarioNameAr(command.scenarioNameAr());
        rule.setTriggerRiskStatus(command.triggerRiskStatus());
        rule.setTriggerPepFlag(command.triggerPepFlag());
        rule.setTriggerThirdPartyCheckType(command.triggerThirdPartyCheckType());
        rule.setTriggerThirdPartyResult(command.triggerThirdPartyResult());
        rule.setResultingAccountStatus(command.resultingAccountStatus());
        rule.setResultingComplianceStatus(command.resultingComplianceStatus());
        rule.setRequiresManualReview(command.requiresManualReview());
        rule.setNotifyRole(command.notifyRole());
        rule.setPriority(command.priority());
        rule.setSlaDurationHours(command.slaDurationHours());
        rule.setActive(true);
        rule.setCreatedAt(Instant.now());
        rule.setUpdatedAt(Instant.now());
        rule.setVersion(1);

        var saved = scenarioRuleRepository.save(rule);
        log.info("Scenario rule created: {} for tenant: {}", command.scenarioName(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public ScenarioRule update(UUID tenantId, UUID ruleId, UpdateScenarioRuleCommand command) {
        var rule = scenarioRuleRepository.findById(tenantId, ruleId)
                .orElseThrow(() -> NotFoundException.forEntity("ScenarioRule", ruleId.toString()));

        if (command.scenarioName() != null) rule.setScenarioName(command.scenarioName());
        if (command.scenarioNameAr() != null) rule.setScenarioNameAr(command.scenarioNameAr());
        if (command.triggerRiskStatus() != null) rule.setTriggerRiskStatus(command.triggerRiskStatus());
        if (command.triggerPepFlag() != null) rule.setTriggerPepFlag(command.triggerPepFlag());
        if (command.triggerThirdPartyCheckType() != null) rule.setTriggerThirdPartyCheckType(command.triggerThirdPartyCheckType());
        if (command.triggerThirdPartyResult() != null) rule.setTriggerThirdPartyResult(command.triggerThirdPartyResult());
        if (command.resultingAccountStatus() != null) rule.setResultingAccountStatus(command.resultingAccountStatus());
        if (command.resultingComplianceStatus() != null) rule.setResultingComplianceStatus(command.resultingComplianceStatus());
        if (command.requiresManualReview() != null) rule.setRequiresManualReview(command.requiresManualReview());
        if (command.notifyRole() != null) rule.setNotifyRole(command.notifyRole());
        if (command.priority() != null) rule.setPriority(command.priority());
        if (command.slaDurationHours() != null) rule.setSlaDurationHours(command.slaDurationHours());
        rule.setUpdatedAt(Instant.now());

        var saved = scenarioRuleRepository.save(rule);
        log.info("Scenario rule updated: {}", ruleId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ScenarioRule getById(UUID tenantId, UUID ruleId) {
        return scenarioRuleRepository.findById(tenantId, ruleId)
                .orElseThrow(() -> NotFoundException.forEntity("ScenarioRule", ruleId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScenarioRule> getAll(UUID tenantId) {
        return scenarioRuleRepository.findAllByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScenarioRule> getAll(UUID tenantId, PageQuery query) {
        return scenarioRuleRepository.findAllByTenantId(tenantId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScenarioRule> getActive(UUID tenantId) {
        return scenarioRuleRepository.findActiveByTenantIdOrderByPriority(tenantId);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID ruleId) {
        var rule = getById(tenantId, ruleId);
        rule.setActive(false);
        rule.setUpdatedAt(Instant.now());
        scenarioRuleRepository.save(rule);
        log.info("Scenario rule deactivated: {}", ruleId);
    }
}
