package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.EligibilityFieldDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaEligibilityFieldDefinitionRepository extends JpaRepository<EligibilityFieldDefinitionJpaEntity, UUID> {

    List<EligibilityFieldDefinitionJpaEntity> findByTenantIdOrderBySortOrder(UUID tenantId);

    List<EligibilityFieldDefinitionJpaEntity> findByTenantIdAndActiveOrderBySortOrder(UUID tenantId, boolean active);

    Optional<EligibilityFieldDefinitionJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<EligibilityFieldDefinitionJpaEntity> findByTenantIdAndFieldKey(UUID tenantId, String fieldKey);

    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
