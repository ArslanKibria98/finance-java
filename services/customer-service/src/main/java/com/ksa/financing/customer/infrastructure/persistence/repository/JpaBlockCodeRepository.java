package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.infrastructure.persistence.entity.BlockCodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBlockCodeRepository extends JpaRepository<BlockCodeJpaEntity, UUID> {
    List<BlockCodeJpaEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);
    Optional<BlockCodeJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);
}
