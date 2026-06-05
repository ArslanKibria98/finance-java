package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.model.parameter.ScoringThreshold;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoringThresholdRepository {

    ScoringThreshold save(ScoringThreshold threshold);

    Optional<ScoringThreshold> findById(UUID tenantId, UUID id);

    List<ScoringThreshold> findByRiskType(UUID tenantId, RiskType riskType);

    List<ScoringThreshold> findActiveByRiskType(UUID tenantId, RiskType riskType);
}
