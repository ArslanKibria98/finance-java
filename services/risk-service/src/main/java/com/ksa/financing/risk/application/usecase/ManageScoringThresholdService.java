package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.model.parameter.ScoringThreshold;
import com.ksa.financing.risk.domain.port.in.ManageScoringThresholdUseCase;
import com.ksa.financing.risk.domain.port.out.ScoringThresholdRepository;
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
public class ManageScoringThresholdService implements ManageScoringThresholdUseCase {

    private final ScoringThresholdRepository scoringThresholdRepository;

    @Override
    @Transactional
    public ScoringThreshold create(UUID tenantId, CreateThresholdCommand command) {
        var threshold = new ScoringThreshold();
        threshold.setId(UUID.randomUUID());
        threshold.setTenantId(tenantId);
        threshold.setRiskType(command.riskType());
        threshold.setRiskLevel(command.riskLevel());
        threshold.setMinScore(command.minScore());
        threshold.setMaxScore(command.maxScore());
        threshold.setDescriptionEn(command.descriptionEn());
        threshold.setDescriptionAr(command.descriptionAr());
        threshold.setActive(true);
        threshold.setCreatedAt(Instant.now());
        threshold.setUpdatedAt(Instant.now());
        threshold.setVersion(1);

        var saved = scoringThresholdRepository.save(threshold);
        log.info("Scoring threshold created: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public ScoringThreshold update(UUID tenantId, UUID thresholdId, UpdateThresholdCommand command) {
        var threshold = scoringThresholdRepository.findById(tenantId, thresholdId)
                .orElseThrow(() -> NotFoundException.forEntity("ScoringThreshold", thresholdId.toString()));

        if (command.riskLevel() != null) threshold.setRiskLevel(command.riskLevel());
        if (command.minScore() != null) threshold.setMinScore(command.minScore());
        if (command.maxScore() != null) threshold.setMaxScore(command.maxScore());
        if (command.descriptionEn() != null) threshold.setDescriptionEn(command.descriptionEn());
        if (command.descriptionAr() != null) threshold.setDescriptionAr(command.descriptionAr());
        threshold.setUpdatedAt(Instant.now());

        var saved = scoringThresholdRepository.save(threshold);
        log.info("Scoring threshold updated: {}", thresholdId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoringThreshold> getByRiskType(UUID tenantId, RiskType riskType) {
        return scoringThresholdRepository.findByRiskType(tenantId, riskType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoringThreshold> getActiveByRiskType(UUID tenantId, RiskType riskType) {
        return scoringThresholdRepository.findActiveByRiskType(tenantId, riskType);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID thresholdId) {
        var threshold = scoringThresholdRepository.findById(tenantId, thresholdId)
                .orElseThrow(() -> NotFoundException.forEntity("ScoringThreshold", thresholdId.toString()));
        threshold.setActive(false);
        threshold.setUpdatedAt(Instant.now());
        scoringThresholdRepository.save(threshold);
        log.info("Scoring threshold deactivated: {}", thresholdId);
    }
}
