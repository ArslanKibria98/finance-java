package com.ksa.financing.globalprofile.infrastructure.persistence.repository;

import com.ksa.financing.globalprofile.infrastructure.persistence.entity.RegionalProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RegionalProfileJpaEntity}.
 * <p>
 * Supports lookups by global UID + country code (unique constraint),
 * CIF number lookups for regional identity, and batch queries by global UID.
 */
@Repository
public interface JpaRegionalProfileRepository extends JpaRepository<RegionalProfileJpaEntity, UUID> {

    Optional<RegionalProfileJpaEntity> findByGlobalUidAndCountryCode(UUID globalUid, String countryCode);

    List<RegionalProfileJpaEntity> findAllByGlobalUid(UUID globalUid);

    Optional<RegionalProfileJpaEntity> findByCountryCodeAndRegionalCifNumber(String countryCode, String cifNumber);
}
