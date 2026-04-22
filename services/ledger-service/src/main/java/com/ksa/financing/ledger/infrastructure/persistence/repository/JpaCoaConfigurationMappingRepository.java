package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaConfigurationMappingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCoaConfigurationMappingRepository extends JpaRepository<CoaConfigurationMappingJpaEntity, UUID> {

    List<CoaConfigurationMappingJpaEntity> findAllByTenantIdAndProfileId(UUID tenantId, UUID profileId);

    void deleteAllByTenantIdAndProfileId(UUID tenantId, UUID profileId);
}
