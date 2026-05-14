package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ApiRequestLogJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRequestLogRepository extends JpaRepository<ApiRequestLogJpaEntity, UUID>,
        JpaSpecificationExecutor<ApiRequestLogJpaEntity> {

    Optional<ApiRequestLogJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ApiRequestLogJpaEntity> findByRequestIdAndTenantId(String requestId, UUID tenantId);

    Optional<ApiRequestLogJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);

    List<ApiRequestLogJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<ApiRequestLogJpaEntity> findByApiIdInAndTenantIdOrderByCreatedAtDesc(List<UUID> apiIds, UUID tenantId, Pageable pageable);
}
