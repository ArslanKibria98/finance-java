package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.ScoringThresholdJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.entity.RiskParameterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaScoringThresholdRepository extends JpaRepository<ScoringThresholdJpaEntity, UUID> {
    Optional<ScoringThresholdJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ScoringThresholdJpaEntity> findAllByTenantIdAndRiskType(UUID tenantId, RiskParameterJpaEntity.RiskTypeEnum riskType);
    List<ScoringThresholdJpaEntity> findAllByTenantIdAndRiskTypeAndActiveTrue(UUID tenantId, RiskParameterJpaEntity.RiskTypeEnum riskType);
}
