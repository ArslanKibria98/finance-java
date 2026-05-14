package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.FineractAuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaFineractAuditLogRepository extends JpaRepository<FineractAuditLogJpaEntity, UUID> {

    List<FineractAuditLogJpaEntity> findByTenantIdAndOperationOrderByOccurredAtDesc(UUID tenantId, String operation);

    List<FineractAuditLogJpaEntity> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);
}
