package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.EmployeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaEmployeeRepository extends JpaRepository<EmployeeJpaEntity, UUID> {
    Optional<EmployeeJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    List<EmployeeJpaEntity> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    Optional<EmployeeJpaEntity> findByKeycloakUserId(UUID keycloakUserId);
}
