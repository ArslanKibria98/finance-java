package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ApiClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRepository extends JpaRepository<ApiClientJpaEntity, UUID> {

    Optional<ApiClientJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<ApiClientJpaEntity> findByCodeAndTenantIdAndDeletedAtIsNull(String code, UUID tenantId);

    List<ApiClientJpaEntity> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);

    boolean existsByCodeAndTenantIdAndDeletedAtIsNull(String code, UUID tenantId);
}
