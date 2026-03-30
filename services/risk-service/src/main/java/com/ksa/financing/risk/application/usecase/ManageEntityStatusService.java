package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;
import com.ksa.financing.risk.domain.model.status.EntityStatusRecord;
import com.ksa.financing.risk.domain.port.in.ManageEntityStatusUseCase;
import com.ksa.financing.risk.domain.port.out.EntityStatusRepository;
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
public class ManageEntityStatusService implements ManageEntityStatusUseCase {

    private final EntityStatusRepository entityStatusRepository;

    @Override
    @Transactional(readOnly = true)
    public EntityStatusRecord getCurrentStatus(UUID tenantId, String entityReference) {
        return entityStatusRepository.findLatestByEntityReference(tenantId, entityReference)
                .orElseThrow(() -> NotFoundException.forEntity("EntityStatus", entityReference));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityStatusRecord> getStatusHistory(UUID tenantId, String entityReference) {
        return entityStatusRepository.findByEntityReference(tenantId, entityReference);
    }

    @Override
    @Transactional
    public EntityStatusRecord updateAccountStatus(UUID tenantId, String entityReference,
                                                   AccountStatus newStatus, String reason, UUID changedBy) {
        var current = entityStatusRepository.findLatestByEntityReference(tenantId, entityReference)
                .orElse(null);

        var record = new EntityStatusRecord();
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        record.setEntityReference(entityReference);
        record.setAccountStatus(newStatus);
        record.setComplianceStatus(current != null ? current.getComplianceStatus() : ComplianceStatus.PENDING);
        record.setRiskStatus(current != null ? current.getRiskStatus() : null);
        record.setStatusReason(reason);
        record.setChangedBy(changedBy);
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        record.setVersion(1);

        var saved = entityStatusRepository.save(record);
        log.info("Account status updated: {} -> {} for entity: {}",
                current != null ? current.getAccountStatus() : "NEW", newStatus, entityReference);
        return saved;
    }

    @Override
    @Transactional
    public EntityStatusRecord updateComplianceStatus(UUID tenantId, String entityReference,
                                                      ComplianceStatus newStatus, String reason, UUID changedBy) {
        var current = entityStatusRepository.findLatestByEntityReference(tenantId, entityReference)
                .orElse(null);

        var record = new EntityStatusRecord();
        record.setId(UUID.randomUUID());
        record.setTenantId(tenantId);
        record.setEntityReference(entityReference);
        record.setAccountStatus(current != null ? current.getAccountStatus() : AccountStatus.PENDING);
        record.setComplianceStatus(newStatus);
        record.setRiskStatus(current != null ? current.getRiskStatus() : null);
        record.setStatusReason(reason);
        record.setChangedBy(changedBy);
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        record.setVersion(1);

        var saved = entityStatusRepository.save(record);
        log.info("Compliance status updated: {} -> {} for entity: {}",
                current != null ? current.getComplianceStatus() : "NEW", newStatus, entityReference);
        return saved;
    }
}
