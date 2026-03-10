package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.AdminFeeSlabJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAdminFeeSlabRepository extends JpaRepository<AdminFeeSlabJpaEntity, UUID> {

    List<AdminFeeSlabJpaEntity> findByProductIdAndTenantIdOrderBySortOrder(UUID productId, UUID tenantId);

    void deleteByProductIdAndTenantId(UUID productId, UUID tenantId);
}
