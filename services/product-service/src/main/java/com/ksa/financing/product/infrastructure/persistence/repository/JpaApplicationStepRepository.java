package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ApplicationStepJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaApplicationStepRepository extends JpaRepository<ApplicationStepJpaEntity, UUID> {

    List<ApplicationStepJpaEntity> findByProductIdAndTenantIdOrderBySortOrder(UUID productId, UUID tenantId);

    void deleteByProductIdAndTenantId(UUID productId, UUID tenantId);
}
