package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.CreditScoringFieldDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCreditScoringFieldDefinitionRepository extends JpaRepository<CreditScoringFieldDefinitionJpaEntity, UUID> {

    List<CreditScoringFieldDefinitionJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrder(UUID tenantId);
}
