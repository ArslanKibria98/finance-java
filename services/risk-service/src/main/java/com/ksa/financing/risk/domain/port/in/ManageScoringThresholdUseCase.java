package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.model.parameter.ScoringThreshold;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageScoringThresholdUseCase {

    ScoringThreshold create(UUID tenantId, CreateThresholdCommand command);

    ScoringThreshold update(UUID tenantId, UUID thresholdId, UpdateThresholdCommand command);

    List<ScoringThreshold> getByRiskType(UUID tenantId, RiskType riskType);

    List<ScoringThreshold> getActiveByRiskType(UUID tenantId, RiskType riskType);

    void deactivate(UUID tenantId, UUID thresholdId);

    record CreateThresholdCommand(
            RiskType riskType,
            String riskLevel,
            BigDecimal minScore,
            BigDecimal maxScore,
            String descriptionEn,
            String descriptionAr
    ) {}

    record UpdateThresholdCommand(
            String riskLevel,
            BigDecimal minScore,
            BigDecimal maxScore,
            String descriptionEn,
            String descriptionAr
    ) {}
}
