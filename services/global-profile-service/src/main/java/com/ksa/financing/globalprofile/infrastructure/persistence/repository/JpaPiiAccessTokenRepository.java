package com.ksa.financing.globalprofile.infrastructure.persistence.repository;

import com.ksa.financing.globalprofile.infrastructure.persistence.entity.PiiAccessTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PiiAccessTokenJpaEntity}.
 * <p>
 * PII access tokens are time-limited (max 10 minutes) and used to
 * authorize reads from the PII Vault service.
 */
@Repository
public interface JpaPiiAccessTokenRepository extends JpaRepository<PiiAccessTokenJpaEntity, UUID> {

    Optional<PiiAccessTokenJpaEntity> findByTokenHash(String tokenHash);
}
