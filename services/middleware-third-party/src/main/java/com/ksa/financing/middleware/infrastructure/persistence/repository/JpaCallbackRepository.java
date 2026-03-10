package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.CallbackResponseJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCallbackRepository extends JpaRepository<CallbackResponseJpaEntity, UUID> {

    Optional<CallbackResponseJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CallbackResponseJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
