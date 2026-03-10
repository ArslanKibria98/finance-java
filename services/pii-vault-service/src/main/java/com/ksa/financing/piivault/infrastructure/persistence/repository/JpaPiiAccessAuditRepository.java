package com.ksa.financing.piivault.infrastructure.persistence.repository;

import com.ksa.financing.piivault.infrastructure.persistence.entity.PiiAccessAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PiiAccessAuditJpaEntity}.
 * <p>
 * This is a WORM (Write Once Read Many) table — only INSERT operations.
 * The DB triggers prevent UPDATE and DELETE for compliance. The hash chain
 * is computed by the DB trigger {@code chain_audit_log} on INSERT.
 */
@Repository
public interface JpaPiiAccessAuditRepository extends JpaRepository<PiiAccessAuditJpaEntity, UUID> {

    /**
     * Finds the most recent audit entry for a given global UID.
     * Used for retrieving the latest hash in the chain.
     */
    Optional<PiiAccessAuditJpaEntity> findTopByGlobalUidOrderByAccessedAtDesc(UUID globalUid);
}
