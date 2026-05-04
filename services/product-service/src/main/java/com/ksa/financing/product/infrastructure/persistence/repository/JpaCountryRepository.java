package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.CountryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCountryRepository extends JpaRepository<CountryJpaEntity, UUID>, JpaSpecificationExecutor<CountryJpaEntity> {

    List<CountryJpaEntity> findAllByTenantIdAndIsActiveTrue(UUID tenantId);

    List<CountryJpaEntity> findAllByTenantIdAndIsGccTrueAndIsActiveTrue(UUID tenantId);

    List<CountryJpaEntity> findAllByTenantIdAndIsArabLeagueTrueAndIsActiveTrue(UUID tenantId);

    List<CountryJpaEntity> findAllByTenantIdAndIsSanctionedTrue(UUID tenantId);

    List<CountryJpaEntity> findAllByTenantIdAndRegionAndIsActiveTrue(UUID tenantId, String region);

    List<CountryJpaEntity> findAllByTenantIdAndRiskTierAndIsActiveTrue(UUID tenantId, String riskTier);

    Optional<CountryJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<CountryJpaEntity> findByTenantIdAndSlug(UUID tenantId, String slug);
}
