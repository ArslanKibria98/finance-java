package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.EntityStatusJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEntityStatusRepository extends JpaRepository<EntityStatusJpaEntity, UUID> {
    Optional<EntityStatusJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<EntityStatusJpaEntity> findFirstByTenantIdAndEntityReferenceOrderByCreatedAtDesc(UUID tenantId, String entityReference);
    List<EntityStatusJpaEntity> findAllByTenantIdAndEntityReferenceOrderByCreatedAtDesc(UUID tenantId, String entityReference);
}
