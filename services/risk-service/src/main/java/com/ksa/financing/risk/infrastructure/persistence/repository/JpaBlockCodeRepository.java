package com.ksa.financing.risk.infrastructure.persistence.repository;

import com.ksa.financing.risk.domain.model.BlockCodeType;
import com.ksa.financing.risk.infrastructure.persistence.entity.BlockCodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBlockCodeRepository extends JpaRepository<BlockCodeJpaEntity, UUID> {
    Optional<BlockCodeJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<BlockCodeJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);
    List<BlockCodeJpaEntity> findAllByTenantId(UUID tenantId);
    List<BlockCodeJpaEntity> findAllByTenantIdAndType(UUID tenantId, BlockCodeType type);
    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
