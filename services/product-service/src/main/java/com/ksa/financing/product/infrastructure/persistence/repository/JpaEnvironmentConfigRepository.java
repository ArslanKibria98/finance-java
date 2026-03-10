package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.EnvironmentConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEnvironmentConfigRepository extends JpaRepository<EnvironmentConfigJpaEntity, UUID> {

    List<EnvironmentConfigJpaEntity> findByTenantIdAndActiveTrue(UUID tenantId);
}
