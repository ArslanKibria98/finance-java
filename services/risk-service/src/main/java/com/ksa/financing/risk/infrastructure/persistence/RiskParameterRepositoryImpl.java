package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.parameter.RiskParameter;
import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.port.out.RiskParameterRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.RiskParameterJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RiskParameterRepositoryImpl implements RiskParameterRepository {
    private final JpaRiskParameterRepository jpa;

    @Override
    public RiskParameter save(RiskParameter parameter) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(parameter)));
    }
    @Override
    public Optional<RiskParameter> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<RiskParameter> findByRiskType(UUID tenantId, RiskType riskType) {
        return jpa.findAllByTenantIdAndRiskType(tenantId, RiskParameterJpaEntity.RiskTypeEnum.valueOf(riskType.name()))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<RiskParameter> findActiveByRiskType(UUID tenantId, RiskType riskType) {
        if (tenantId == null) {
            return jpa.findAllByRiskTypeAndActiveTrue(RiskParameterJpaEntity.RiskTypeEnum.valueOf(riskType.name()))
                    .stream().map(RiskPersistenceMapper::toDomain).toList();
        }
        return jpa.findAllByTenantIdAndRiskTypeAndActiveTrue(tenantId, RiskParameterJpaEntity.RiskTypeEnum.valueOf(riskType.name()))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<RiskParameter> findByCategory(UUID tenantId, RiskType riskType, String category) {
        return jpa.findAllByTenantIdAndRiskTypeAndCategory(tenantId, RiskParameterJpaEntity.RiskTypeEnum.valueOf(riskType.name()), category)
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<RiskParameter> findByParentId(UUID tenantId, UUID parentParameterId) {
        return jpa.findAllByTenantIdAndParentParameterId(tenantId, parentParameterId)
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
}
