package com.ksa.financing.piivault.infrastructure.persistence.repository;

import com.ksa.financing.piivault.domain.model.PiiAccessAudit;
import com.ksa.financing.piivault.domain.port.out.AuditRepository;
import com.ksa.financing.piivault.infrastructure.persistence.entity.PiiAccessAuditJpaEntity;
import com.ksa.financing.piivault.infrastructure.persistence.mapper.PiiPersistenceMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter implementing {@link AuditRepository}.
 * <p>
 * The pii_access_audits table is IMMUTABLE (WORM compliance):
 * <ul>
 *   <li>Only INSERT operations are allowed</li>
 *   <li>DB triggers prevent UPDATE and DELETE</li>
 *   <li>Hash chaining is computed by the DB trigger {@code chain_audit_log}</li>
 * </ul>
 * After saving, we flush + refresh to pick up the DB-computed hash chain values
 * (previous_log_hash and current_log_hash).
 */
@Component
@RequiredArgsConstructor
public class AuditRepositoryImpl implements AuditRepository {

    private final JpaPiiAccessAuditRepository jpaRepository;
    private final PiiPersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public PiiAccessAudit save(PiiAccessAudit audit) {
        PiiAccessAuditJpaEntity entity = mapper.toEntity(audit);
        PiiAccessAuditJpaEntity saved = jpaRepository.save(entity);

        // Flush to trigger DB-level hash chain computation, then refresh
        entityManager.flush();
        entityManager.refresh(saved);

        return mapper.toDomain(saved);
    }
}
