package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.model.parameter.ScoringThreshold;
import com.ksa.financing.risk.domain.port.out.ScoringThresholdRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.RiskParameterJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ScoringThresholdRepositoryImpl implements ScoringThresholdRepository {
    private final JpaScoringThresholdRepository jpa;

    @Override
    public ScoringThreshold save(ScoringThreshold threshold) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(threshold)));
    }
    @Override
    public Optional<ScoringThreshold> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<ScoringThreshold> findByRiskType(UUID tenantId, RiskType riskType) {
        return jpa.findAllByTenantIdAndRiskType(tenantId, RiskParameterJpaEntity.RiskTypeEnum.valueOf(riskType.name()))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<ScoringThreshold> findActiveByRiskType(UUID tenantId, RiskType riskType) {
        if (tenantId == null) {
            return jpa.findAllByRiskTypeAndActiveTrue(RiskParameterJpaEntity.RiskTypeEnum.valueOf(riskType.name()))
                    .stream().map(RiskPersistenceMapper::toDomain).toList();
        }
        return jpa.findAllByTenantIdAndRiskTypeAndActiveTrue(tenantId, RiskParameterJpaEntity.RiskTypeEnum.valueOf(riskType.name()))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
}
