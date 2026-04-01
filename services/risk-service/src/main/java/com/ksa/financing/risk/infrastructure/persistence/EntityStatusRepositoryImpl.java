package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.status.EntityStatusRecord;
import com.ksa.financing.risk.domain.port.out.EntityStatusRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EntityStatusRepositoryImpl implements EntityStatusRepository {
    private final JpaEntityStatusRepository jpa;

    @Override
    public EntityStatusRecord save(EntityStatusRecord record) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(record)));
    }
    @Override
    public Optional<EntityStatusRecord> findLatestByEntityReference(UUID tenantId, String entityReference) {
        if (tenantId == null) {
            return jpa.findFirstByEntityReferenceOrderByCreatedAtDesc(entityReference).map(RiskPersistenceMapper::toDomain);
        }
        return jpa.findFirstByTenantIdAndEntityReferenceOrderByCreatedAtDesc(tenantId, entityReference).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<EntityStatusRecord> findByEntityReference(UUID tenantId, String entityReference) {
        if (tenantId == null) {
            return jpa.findAllByEntityReferenceOrderByCreatedAtDesc(entityReference).stream().map(RiskPersistenceMapper::toDomain).toList();
        }
        return jpa.findAllByTenantIdAndEntityReferenceOrderByCreatedAtDesc(tenantId, entityReference).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public Optional<EntityStatusRecord> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
}
