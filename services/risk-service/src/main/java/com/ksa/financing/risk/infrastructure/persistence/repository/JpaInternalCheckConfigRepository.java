package com.ksa.financing.risk.infrastructure.persistence.repository;

import com.ksa.financing.risk.infrastructure.persistence.entity.InternalCheckConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInternalCheckConfigRepository extends JpaRepository<InternalCheckConfigJpaEntity, UUID> {
    List<InternalCheckConfigJpaEntity> findAllByTenantId(UUID tenantId);
    Optional<InternalCheckConfigJpaEntity> findByTenantIdAndCheckName(UUID tenantId, String checkName);
}
