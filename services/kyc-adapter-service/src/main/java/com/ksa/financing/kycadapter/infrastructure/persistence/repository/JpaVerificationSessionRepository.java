package com.ksa.financing.kycadapter.infrastructure.persistence.repository;

import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for verification session entities.
 */
@Repository
public interface JpaVerificationSessionRepository extends JpaRepository<VerificationSessionJpaEntity, UUID> {

    Optional<VerificationSessionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<VerificationSessionJpaEntity> findBySessionNumberAndTenantId(String sessionNumber, UUID tenantId);

    Optional<VerificationSessionJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);

    List<VerificationSessionJpaEntity> findByCustomerIdAndTenantId(UUID customerId, UUID tenantId);
}
