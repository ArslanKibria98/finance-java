package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.CountryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCountryRepository extends JpaRepository<CountryJpaEntity, UUID> {

    List<CountryJpaEntity> findAllByTenantIdAndIsActiveTrue(UUID tenantId);

    List<CountryJpaEntity> findAllByTenantIdAndIsGccTrueAndIsActiveTrue(UUID tenantId);

    Optional<CountryJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);
}
