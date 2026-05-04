package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.AuditEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAuditEntryRepository extends JpaRepository<AuditEntryJpaEntity, UUID>, JpaSpecificationExecutor<AuditEntryJpaEntity> {
    List<AuditEntryJpaEntity> findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(UUID tenantId, AuditEntryJpaEntity.AuditEntityTypeEnum entityType, UUID entityId);
    List<AuditEntryJpaEntity> findAllByTenantIdAndActorIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID tenantId, UUID actorId, OffsetDateTime from, OffsetDateTime to);
    List<AuditEntryJpaEntity> findAllByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID tenantId, OffsetDateTime from, OffsetDateTime to);
    List<AuditEntryJpaEntity> findAllByTenantIdAndCorrelationIdOrderByCreatedAtDesc(UUID tenantId, String correlationId);
}
