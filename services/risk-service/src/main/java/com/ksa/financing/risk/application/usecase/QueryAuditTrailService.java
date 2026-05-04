package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;
import com.ksa.financing.risk.domain.port.in.QueryAuditTrailUseCase;
import com.ksa.financing.risk.domain.port.out.AuditEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryAuditTrailService implements QueryAuditTrailUseCase {

    private final AuditEntryRepository auditEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> getByEntity(UUID tenantId, AuditEntityType entityType, UUID entityId, PageQuery query) {
        return auditEntryRepository.findByEntity(tenantId, entityType, entityId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> getByActor(UUID tenantId, UUID actorId, Instant from, Instant to, PageQuery query) {
        return auditEntryRepository.findByActor(tenantId, actorId, from, to, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> getByDateRange(UUID tenantId, Instant from, Instant to, PageQuery query) {
        return auditEntryRepository.findByDateRange(tenantId, from, to, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> getByCorrelationId(UUID tenantId, String correlationId, PageQuery query) {
        return auditEntryRepository.findByCorrelationId(tenantId, correlationId, query);
    }
}
