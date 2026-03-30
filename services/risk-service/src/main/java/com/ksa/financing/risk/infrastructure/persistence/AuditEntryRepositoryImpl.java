package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;
import com.ksa.financing.risk.domain.port.out.AuditEntryRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.AuditEntryJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditEntryRepositoryImpl implements AuditEntryRepository {
    private final JpaAuditEntryRepository jpa;

    @Override
    public AuditEntry save(AuditEntry entry) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(entry)));
    }
    @Override
    public List<AuditEntry> findByEntity(UUID tenantId, AuditEntityType entityType, UUID entityId) {
        return jpa.findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(tenantId, AuditEntryJpaEntity.AuditEntityTypeEnum.valueOf(entityType.name()), entityId)
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<AuditEntry> findByActor(UUID tenantId, UUID actorId, Instant from, Instant to) {
        return jpa.findAllByTenantIdAndActorIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, actorId, from.atOffset(ZoneOffset.UTC), to.atOffset(ZoneOffset.UTC))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<AuditEntry> findByDateRange(UUID tenantId, Instant from, Instant to) {
        return jpa.findAllByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, from.atOffset(ZoneOffset.UTC), to.atOffset(ZoneOffset.UTC))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<AuditEntry> findByCorrelationId(UUID tenantId, String correlationId) {
        return jpa.findAllByTenantIdAndCorrelationIdOrderByCreatedAtDesc(tenantId, correlationId)
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
}
