package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.tenant.TenantConfig;
import com.ksa.financing.risk.domain.port.out.TenantConfigRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TenantConfigRepositoryImpl implements TenantConfigRepository {
    private final JpaTenantConfigRepository jpa;

    @Override
    public TenantConfig save(TenantConfig config) {
        var saved = jpa.save(RiskPersistenceMapper.toEntity(config));
        return RiskPersistenceMapper.toDomain(saved);
    }
    @Override
    public Optional<TenantConfig> findById(UUID tenantId, UUID id) {
        return jpa.findById(id).filter(e -> e.getTenantId().equals(tenantId)).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public Optional<TenantConfig> findByTenantId(UUID tenantId) {
        return jpa.findByTenantId(tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<TenantConfig> findAll() {
        return jpa.findAll().stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public boolean existsByTenantId(UUID tenantId) {
        return jpa.existsByTenantId(tenantId);
    }
}
