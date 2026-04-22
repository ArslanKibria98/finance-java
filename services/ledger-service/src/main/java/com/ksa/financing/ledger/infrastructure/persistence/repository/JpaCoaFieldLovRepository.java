package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaFieldLovJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCoaFieldLovRepository extends JpaRepository<CoaFieldLovJpaEntity, UUID> {

    Optional<CoaFieldLovJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<CoaFieldLovJpaEntity> findByTenantIdAndFieldKey(UUID tenantId, String fieldKey);

    List<CoaFieldLovJpaEntity> findAllByTenantIdOrderByDisplayOrderAsc(UUID tenantId);

    List<CoaFieldLovJpaEntity> findAllByTenantIdAndStatusOrderByDisplayOrderAsc(UUID tenantId, String status);

    boolean existsByTenantIdAndFieldKey(UUID tenantId, String fieldKey);
}
