package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.PartnerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPartnerRepository extends JpaRepository<PartnerJpaEntity, UUID> {

    Optional<PartnerJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<PartnerJpaEntity> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    boolean existsByTenantIdAndPartnerCode(UUID tenantId, String partnerCode);
}
