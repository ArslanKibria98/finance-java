package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.EmployeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaEmployeeRepository extends JpaRepository<EmployeeJpaEntity, UUID>, JpaSpecificationExecutor<EmployeeJpaEntity> {
    Optional<EmployeeJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    Optional<EmployeeJpaEntity> findByKeycloakUserId(UUID keycloakUserId);
}
