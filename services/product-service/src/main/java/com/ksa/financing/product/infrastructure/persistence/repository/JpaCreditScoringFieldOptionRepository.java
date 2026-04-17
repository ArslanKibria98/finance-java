package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.CreditScoringFieldOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCreditScoringFieldOptionRepository extends JpaRepository<CreditScoringFieldOptionJpaEntity, UUID> {

    List<CreditScoringFieldOptionJpaEntity> findByTenantIdAndFieldDefinitionIdAndActiveTrueOrderBySortOrder(
            UUID tenantId, UUID fieldDefinitionId);

    List<CreditScoringFieldOptionJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrder(UUID tenantId);
}
