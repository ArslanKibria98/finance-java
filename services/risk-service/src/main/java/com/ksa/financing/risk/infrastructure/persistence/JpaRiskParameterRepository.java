package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.RiskParameterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRiskParameterRepository extends JpaRepository<RiskParameterJpaEntity, UUID> {
    Optional<RiskParameterJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<RiskParameterJpaEntity> findAllByTenantIdAndRiskType(UUID tenantId, RiskParameterJpaEntity.RiskTypeEnum riskType);
    List<RiskParameterJpaEntity> findAllByTenantIdAndRiskTypeAndActiveTrue(UUID tenantId, RiskParameterJpaEntity.RiskTypeEnum riskType);
    List<RiskParameterJpaEntity> findAllByRiskTypeAndActiveTrue(RiskParameterJpaEntity.RiskTypeEnum riskType);
    List<RiskParameterJpaEntity> findAllByTenantIdAndRiskTypeAndCategory(UUID tenantId, RiskParameterJpaEntity.RiskTypeEnum riskType, String category);
    List<RiskParameterJpaEntity> findAllByTenantIdAndParentParameterId(UUID tenantId, UUID parentParameterId);
}
