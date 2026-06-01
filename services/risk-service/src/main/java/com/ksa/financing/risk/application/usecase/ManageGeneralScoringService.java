package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CreditScoringOperator;
import com.ksa.financing.risk.domain.model.credit.CreditScoringRule;
import com.ksa.financing.risk.domain.model.credit.GeneralScoringConfig;
import com.ksa.financing.risk.domain.port.in.ManageGeneralScoringUseCase;
import com.ksa.financing.risk.domain.port.out.GeneralCreditScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageGeneralScoringService implements ManageGeneralScoringUseCase {

    private final GeneralCreditScoringRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<CreditScoringCriteria> getCriteria(UUID tenantId) {
        log.info("Fetching general scoring criteria for tenant={}", tenantId);
        return repository.findAllCriteria(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditScoringCriteria getCriteriaById(UUID tenantId, UUID id) {
        return repository.findCriteriaById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "General criterion not found: " + id));
    }

    @Override
    @Transactional
    public CreditScoringCriteria createCriteria(UUID tenantId, GeneralCriteriaCommand cmd) {
        log.info("Creating single general criterion fieldDef={} for tenant={}",
                cmd.fieldDefinitionId(), tenantId);
        var domain = toDomain(tenantId, cmd);
        return repository.saveSingleCriteria(tenantId, domain);
    }

    @Override
    @Transactional
    public CreditScoringCriteria updateCriteria(UUID tenantId, UUID id, GeneralCriteriaCommand cmd) {
        log.info("Updating general criterion id={} for tenant={}", id, tenantId);
        var domain = toDomain(tenantId, cmd);
        return repository.updateSingleCriteria(tenantId, id, domain);
    }

    @Override
    @Transactional
    public void deleteCriteria(UUID tenantId, UUID id) {
        log.info("Deleting general criterion id={} for tenant={}", id, tenantId);
        repository.deleteSingleCriteria(tenantId, id);
    }

    private CreditScoringCriteria toDomain(UUID tenantId, GeneralCriteriaCommand cmd) {
        return new CreditScoringCriteria(
                null,
                tenantId,
                null,
                cmd.fieldDefinitionId(),
                cmd.customName(),
                cmd.custom(),
                cmd.enabled(),
                cmd.sortOrder(),
                cmd.rules() == null ? List.of() : cmd.rules().stream()
                        .map(r -> new CreditScoringRule(
                                null,
                                tenantId,
                                null,
                                CreditScoringOperator.valueOf(r.operator()),
                                r.value(),
                                r.weight(),
                                r.percentage()
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional
    public void saveCriteria(UUID tenantId, List<GeneralCriteriaCommand> commands) {
        log.info("Saving {} general scoring criteria for tenant={}", commands.size(), tenantId);

        var criteria = commands.stream()
                .map(cmd -> new CreditScoringCriteria(
                        null,
                        tenantId,
                        null,                                  // productId is NULL for general scoring
                        cmd.fieldDefinitionId(),
                        cmd.customName(),
                        cmd.custom(),
                        cmd.enabled(),
                        cmd.sortOrder(),
                        cmd.rules() == null ? List.of() : cmd.rules().stream()
                                .map(r -> new CreditScoringRule(
                                        null,
                                        tenantId,
                                        null,
                                        CreditScoringOperator.valueOf(r.operator()),
                                        r.value(),
                                        r.weight(),
                                        r.percentage()
                                ))
                                .toList()
                ))
                .toList();

        repository.saveCriteria(tenantId, criteria);
        log.info("General scoring criteria saved for tenant={}", tenantId);
    }

    @Override
    @Transactional
    public void deleteAllCriteria(UUID tenantId) {
        log.info("Deleting all general scoring criteria for tenant={}", tenantId);
        repository.deleteAllCriteria(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public GeneralScoringConfig getConfig(UUID tenantId) {
        return repository.findConfig(tenantId)
                .orElseGet(() -> repository.saveConfig(GeneralScoringConfig.defaults(tenantId)));
    }

    @Override
    @Transactional
    public GeneralScoringConfig updateConfig(UUID tenantId, UpdateGeneralConfigCommand cmd) {
        log.info("Updating general scoring config for tenant={}: green={}, amber={}, minPass={}",
                tenantId, cmd.greenThreshold(), cmd.amberThreshold(), cmd.minPassPercentage());
        var existing = repository.findConfig(tenantId).orElse(GeneralScoringConfig.defaults(tenantId));
        var updated = new GeneralScoringConfig(
                existing.id(),
                tenantId,
                cmd.minPassPercentage(),
                cmd.greenThreshold(),
                cmd.amberThreshold(),
                cmd.enabled()
        );
        return repository.saveConfig(updated);
    }
}
