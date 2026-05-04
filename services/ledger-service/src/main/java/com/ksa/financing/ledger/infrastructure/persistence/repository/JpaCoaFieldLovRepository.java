package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaFieldLovJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaCoaFieldLovRepository extends JpaRepository<CoaFieldLovJpaEntity, UUID>,
        JpaSpecificationExecutor<CoaFieldLovJpaEntity> {

    Optional<CoaFieldLovJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<CoaFieldLovJpaEntity> findByTenantIdAndFieldKey(UUID tenantId, String fieldKey);

    Page<CoaFieldLovJpaEntity> findAllByTenantIdOrderByDisplayOrderAsc(UUID tenantId, Pageable pageable);

    Page<CoaFieldLovJpaEntity> findAllByTenantIdAndStatusOrderByDisplayOrderAsc(UUID tenantId, String status, Pageable pageable);

    boolean existsByTenantIdAndFieldKey(UUID tenantId, String fieldKey);
}
