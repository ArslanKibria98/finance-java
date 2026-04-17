package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.NetWorthRangeOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaNetWorthRangeRepository extends JpaRepository<NetWorthRangeOptionJpaEntity, UUID> {
    Optional<NetWorthRangeOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    List<NetWorthRangeOptionJpaEntity> findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    List<NetWorthRangeOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
