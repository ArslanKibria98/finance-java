package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaCustomerRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    Optional<CustomerJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<CustomerJpaEntity> findByCifNumberAndTenantIdAndDeletedAtIsNull(String cifNumber, UUID tenantId);

    Optional<CustomerJpaEntity> findByNationalIdAndTenantIdAndDeletedAtIsNull(String nationalId, UUID tenantId);

    Optional<CustomerJpaEntity> findByKeycloakUserId(UUID keycloakUserId);

    Optional<CustomerJpaEntity> findByTenantIdAndIdempotencyKeyAndDeletedAtIsNull(UUID tenantId, String idempotencyKey);

    boolean existsByNationalIdAndTenantIdAndDeletedAtIsNull(String nationalId, UUID tenantId);
}
