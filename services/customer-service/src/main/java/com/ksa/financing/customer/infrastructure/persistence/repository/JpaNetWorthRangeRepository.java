package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.NetWorthRangeOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaNetWorthRangeRepository extends JpaRepository<NetWorthRangeOptionJpaEntity, UUID> {
    Optional<NetWorthRangeOptionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<NetWorthRangeOptionJpaEntity> findByTenantIdOrderByDisplayOrderAsc(UUID tenantId);
    List<NetWorthRangeOptionJpaEntity> findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(UUID tenantId);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
