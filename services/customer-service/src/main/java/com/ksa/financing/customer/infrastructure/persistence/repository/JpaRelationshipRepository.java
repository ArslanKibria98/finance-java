package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.RelationshipOptionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaRelationshipRepository extends JpaRepository<RelationshipOptionJpaEntity, UUID>,
        JpaSpecificationExecutor<RelationshipOptionJpaEntity> {
    Optional<RelationshipOptionJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Page<RelationshipOptionJpaEntity> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    Page<RelationshipOptionJpaEntity> findByTenantIdAndActiveTrueAndDeletedFalse(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
}
