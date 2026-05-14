package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ApiEnvironmentConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEnvConfigRepository extends JpaRepository<ApiEnvironmentConfigJpaEntity, UUID> {

    Optional<ApiEnvironmentConfigJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    List<ApiEnvironmentConfigJpaEntity> findByApiIdAndTenantIdAndDeletedAtIsNull(UUID apiId, UUID tenantId);

    List<ApiEnvironmentConfigJpaEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    @Query("""
            SELECT ec FROM ApiEnvironmentConfigJpaEntity ec
            WHERE ec.tenantId = :tenantId
              AND ec.deletedAt IS NULL
              AND ec.apiId IN (
                  SELECT pa.id FROM ProviderApiJpaEntity pa
                  WHERE pa.providerId = :providerId
                    AND pa.tenantId = :tenantId
                    AND pa.deletedAt IS NULL
              )
            """)
    List<ApiEnvironmentConfigJpaEntity> findByProviderId(@Param("tenantId") UUID tenantId,
                                                          @Param("providerId") UUID providerId);
}
