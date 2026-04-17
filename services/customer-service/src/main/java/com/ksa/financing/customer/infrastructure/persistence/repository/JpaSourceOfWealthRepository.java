package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.SourceOfWealthOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSourceOfWealthRepository extends JpaRepository<SourceOfWealthOptionJpaEntity, UUID> {
    Optional<SourceOfWealthOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<SourceOfWealthOptionJpaEntity> findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    List<SourceOfWealthOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
