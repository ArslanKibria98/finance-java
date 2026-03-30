package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ApiEnvironmentConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEnvConfigRepository extends JpaRepository<ApiEnvironmentConfigJpaEntity, UUID> {

    Optional<ApiEnvironmentConfigJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    List<ApiEnvironmentConfigJpaEntity> findByApiIdAndTenantIdAndDeletedAtIsNull(UUID apiId, UUID tenantId);

    List<ApiEnvironmentConfigJpaEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
