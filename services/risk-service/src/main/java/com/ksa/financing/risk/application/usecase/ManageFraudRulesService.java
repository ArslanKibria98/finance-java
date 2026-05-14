package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.FraudRule;
import com.ksa.financing.risk.domain.port.in.ManageFraudRulesUseCase;
import com.ksa.financing.risk.domain.port.out.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageFraudRulesService implements ManageFraudRulesUseCase {

    private final FraudRuleRepository fraudRuleRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FraudRule> listAll(UUID tenantId, PageQuery query) {
        log.debug("Listing all fraud rules for tenant={} page={} size={}", tenantId, query.page(), query.size());
        return fraudRuleRepository.findAllByTenant(tenantId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FraudRule> listByCategory(UUID tenantId, String category, PageQuery query) {
        log.debug("Listing fraud rules for tenant={} category={} page={} size={}", tenantId, category, query.page(), query.size());
        return fraudRuleRepository.findByCategory(tenantId, category, query);
    }

    @Override
    @Transactional(readOnly = true)
    public FraudRule getById(UUID tenantId, UUID id) {
        return fraudRuleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("FraudRule", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public FraudRule getByRuleId(UUID tenantId, String ruleId) {
        return fraudRuleRepository.findByRuleId(tenantId, ruleId)
                .orElseThrow(() -> NotFoundException.forEntity("FraudRule", ruleId));
    }

    @Override
    @Transactional
    public FraudRule create(UUID tenantId, FraudRule rule) {
        log.info("Creating fraud rule ruleId={} for tenant={}", rule.getRuleId(), tenantId);

        fraudRuleRepository.findByRuleId(tenantId, rule.getRuleId()).ifPresent(existing -> {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Fraud rule with this ruleId already exists: " + rule.getRuleId(), rule.getRuleId());
        });

        rule.setId(UUID.randomUUID());
        rule.setTenantId(tenantId);
        rule.setCreatedAt(Instant.now());
        rule.setUpdatedAt(Instant.now());

        return fraudRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public FraudRule update(UUID tenantId, UUID id, FraudRule updates) {
        log.info("Updating fraud rule id={} for tenant={}", id, tenantId);

        var existing = fraudRuleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("FraudRule", id.toString()));

        existing.setScenarioName(updates.getScenarioName());
        existing.setScenarioNameAr(updates.getScenarioNameAr());
        existing.setCategory(updates.getCategory());
        existing.setDetectionLogic(updates.getDetectionLogic());
        existing.setDefaultAction(updates.getDefaultAction());
        existing.setBlockType(updates.getBlockType());
        existing.setStatus(updates.getStatus());
        existing.setParameters(updates.getParameters());
        existing.setPriority(updates.getPriority());
        existing.setUpdatedAt(Instant.now());

        return fraudRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void assignBlockCode(UUID tenantId, UUID id, UUID blockCodeId) {
        log.info("Assigning block code id={} to fraud rule id={} for tenant={}", blockCodeId, id, tenantId);
        var rule = fraudRuleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("FraudRule", id.toString()));
        rule.setBlockCodeId(blockCodeId);
        rule.setUpdatedAt(Instant.now());
        fraudRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        log.info("Deleting fraud rule id={} for tenant={}", id, tenantId);
        fraudRuleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("FraudRule", id.toString()));
        fraudRuleRepository.delete(id);
    }
}
