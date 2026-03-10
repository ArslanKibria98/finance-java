package com.ksa.financing.globalprofile.infrastructure.persistence.repository;

import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import com.ksa.financing.globalprofile.domain.port.out.RegionalProfileRepository;
import com.ksa.financing.globalprofile.infrastructure.persistence.entity.RegionalProfileJpaEntity;
import com.ksa.financing.globalprofile.infrastructure.persistence.mapper.GlobalProfilePersistenceMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link RegionalProfileRepository}.
 * <p>
 * IMPORTANT: After saving a regional profile, the DB trigger
 * {@code trigger_compute_global_kyc} fires to recompute the global
 * customer's aggregate KYC status. We flush + refresh to capture this.
 */
@Component
@RequiredArgsConstructor
public class RegionalProfileRepositoryImpl implements RegionalProfileRepository {

    private final JpaRegionalProfileRepository jpaRepository;
    private final GlobalProfilePersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public RegionalProfile save(RegionalProfile regionalProfile) {
        RegionalProfileJpaEntity entity = mapper.toEntity(regionalProfile);
        RegionalProfileJpaEntity saved = jpaRepository.save(entity);

        // Flush to trigger DB-level triggers (KYC aggregate recomputation), then refresh
        entityManager.flush();
        entityManager.refresh(saved);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RegionalProfile> findById(UUID regionalProfileId) {
        return jpaRepository.findById(regionalProfileId)
                .map(mapper::toDomain);
    }

    @Override
    public List<RegionalProfile> findByGlobalUid(UUID globalUid) {
        return jpaRepository.findAllByGlobalUid(globalUid).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RegionalProfile> findByGlobalUidAndCountry(UUID globalUid, String countryCode) {
        return jpaRepository.findByGlobalUidAndCountryCode(globalUid, countryCode)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RegionalProfile> findByCifNumber(String countryCode, String cifNumber) {
        return jpaRepository.findByCountryCodeAndRegionalCifNumber(countryCode, cifNumber)
                .map(mapper::toDomain);
    }
}
