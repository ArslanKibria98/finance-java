package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;
import com.ksa.financing.risk.domain.port.in.QueryAuditTrailUseCase;
import com.ksa.financing.risk.domain.port.out.AuditEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryAuditTrailService implements QueryAuditTrailUseCase {

    private final AuditEntryRepository auditEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> getByEntity(UUID tenantId, AuditEntityType entityType, UUID entityId) {
        return auditEntryRepository.findByEntity(tenantId, entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> getByActor(UUID tenantId, UUID actorId, Instant from, Instant to) {
        return auditEntryRepository.findByActor(tenantId, actorId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> getByDateRange(UUID tenantId, Instant from, Instant to) {
        return auditEntryRepository.findByDateRange(tenantId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> getByCorrelationId(UUID tenantId, String correlationId) {
        return auditEntryRepository.findByCorrelationId(tenantId, correlationId);
    }
}
