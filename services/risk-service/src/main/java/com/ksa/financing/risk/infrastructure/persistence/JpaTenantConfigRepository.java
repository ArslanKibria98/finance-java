package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.TenantConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTenantConfigRepository extends JpaRepository<TenantConfigJpaEntity, UUID> {
    Optional<TenantConfigJpaEntity> findByTenantId(UUID tenantId);
    boolean existsByTenantId(UUID tenantId);
}
