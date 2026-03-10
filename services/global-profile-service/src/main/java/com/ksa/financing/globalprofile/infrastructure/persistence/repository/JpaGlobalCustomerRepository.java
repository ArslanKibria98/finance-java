package com.ksa.financing.globalprofile.infrastructure.persistence.repository;

import com.ksa.financing.globalprofile.infrastructure.persistence.entity.GlobalCustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link GlobalCustomerJpaEntity}.
 * <p>
 * Queries on hash columns (globalEmailHash, globalMobileHash) are used
 * for deduplication lookups — the global_customers table stores ZERO PII.
 */
@Repository
public interface JpaGlobalCustomerRepository extends JpaRepository<GlobalCustomerJpaEntity, UUID> {

    Optional<GlobalCustomerJpaEntity> findByGlobalUid(UUID globalUid);

    Optional<GlobalCustomerJpaEntity> findByGlobalEmailHash(String emailHash);

    Optional<GlobalCustomerJpaEntity> findByGlobalMobileHash(String mobileHash);

    boolean existsByGlobalUid(UUID globalUid);
}
