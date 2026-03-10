package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ProviderApiJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProviderApiRepository extends JpaRepository<ProviderApiJpaEntity, UUID> {

    Optional<ProviderApiJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<ProviderApiJpaEntity> findByCodeAndTenantIdAndDeletedAtIsNull(String code, UUID tenantId);

    List<ProviderApiJpaEntity> findByProviderIdAndTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID providerId, UUID tenantId);

    List<ProviderApiJpaEntity> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);

    boolean existsByCodeAndTenantIdAndDeletedAtIsNull(String code, UUID tenantId);
}
