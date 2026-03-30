package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.lov.LovSet;
import com.ksa.financing.risk.domain.port.out.LovSetRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LovSetRepositoryImpl implements LovSetRepository {
    private final JpaLovSetRepository jpa;

    @Override
    public LovSet save(LovSet lovSet) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(lovSet)));
    }
    @Override
    public Optional<LovSet> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<LovSet> findAllByTenantId(UUID tenantId) {
        return jpa.findAllByTenantId(tenantId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<LovSet> findActiveByTenantId(UUID tenantId) {
        return jpa.findAllByTenantIdAndActiveTrue(tenantId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public boolean existsByCode(UUID tenantId, String code) {
        return jpa.existsByTenantIdAndCode(tenantId, code);
    }
}
