package com.ksa.financing.piivault.infrastructure.persistence.repository;

import com.ksa.financing.piivault.infrastructure.persistence.entity.PiiIndividualJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PiiIndividualJpaEntity}.
 * <p>
 * Lookups are scoped by globalUid. The vault_region constraint is enforced
 * at the DB level via CHECK constraint matching the vault_config table.
 */
@Repository
public interface JpaPiiIndividualRepository extends JpaRepository<PiiIndividualJpaEntity, UUID> {

    Optional<PiiIndividualJpaEntity> findByGlobalUidAndDeletedAtIsNull(UUID globalUid);

    boolean existsByGlobalUidAndDeletedAtIsNull(UUID globalUid);
}
