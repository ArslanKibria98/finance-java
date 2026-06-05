package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;

import java.time.Instant;
import java.util.UUID;

public interface AuditEntryRepository {

    AuditEntry save(AuditEntry entry);

    PageResponse<AuditEntry> findByEntity(UUID tenantId, AuditEntityType entityType, UUID entityId, PageQuery query);

    PageResponse<AuditEntry> findByActor(UUID tenantId, UUID actorId, Instant from, Instant to, PageQuery query);

    PageResponse<AuditEntry> findByDateRange(UUID tenantId, Instant from, Instant to, PageQuery query);

    PageResponse<AuditEntry> findByCorrelationId(UUID tenantId, String correlationId, PageQuery query);

    PageResponse<AuditEntry> findAllByTenant(UUID tenantId, PageQuery query);
}
