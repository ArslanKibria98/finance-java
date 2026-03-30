package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.lov.LovEntry;
import com.ksa.financing.risk.domain.port.out.LovEntryRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LovEntryRepositoryImpl implements LovEntryRepository {
    private final JpaLovEntryRepository jpa;

    @Override
    public LovEntry save(LovEntry entry) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(entry)));
    }
    @Override
    public Optional<LovEntry> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<LovEntry> findByLovSetId(UUID tenantId, UUID lovSetId) {
        return jpa.findAllByTenantIdAndLovSetId(tenantId, lovSetId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<LovEntry> findActiveByLovSetId(UUID tenantId, UUID lovSetId) {
        return jpa.findAllByTenantIdAndLovSetIdAndActiveTrue(tenantId, lovSetId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public Optional<LovEntry> findByFactorCode(UUID tenantId, UUID lovSetId, String factorCode) {
        return jpa.findByTenantIdAndLovSetIdAndFactorCode(tenantId, lovSetId, factorCode).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public boolean existsByFactorCode(UUID tenantId, UUID lovSetId, String factorCode) {
        return jpa.existsByTenantIdAndLovSetIdAndFactorCode(tenantId, lovSetId, factorCode);
    }
}
