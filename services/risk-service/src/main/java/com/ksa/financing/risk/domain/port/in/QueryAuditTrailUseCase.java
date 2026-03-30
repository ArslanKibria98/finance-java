package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface QueryAuditTrailUseCase {

    List<AuditEntry> getByEntity(UUID tenantId, AuditEntityType entityType, UUID entityId);

    List<AuditEntry> getByActor(UUID tenantId, UUID actorId, Instant from, Instant to);

    List<AuditEntry> getByDateRange(UUID tenantId, Instant from, Instant to);

    List<AuditEntry> getByCorrelationId(UUID tenantId, String correlationId);
}
