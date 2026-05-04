package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;

import java.time.Instant;
import java.util.UUID;

public interface QueryAuditTrailUseCase {

    PageResponse<AuditEntry> getByEntity(UUID tenantId, AuditEntityType entityType, UUID entityId, PageQuery query);

    PageResponse<AuditEntry> getByActor(UUID tenantId, UUID actorId, Instant from, Instant to, PageQuery query);

    PageResponse<AuditEntry> getByDateRange(UUID tenantId, Instant from, Instant to, PageQuery query);

    PageResponse<AuditEntry> getByCorrelationId(UUID tenantId, String correlationId, PageQuery query);
}
