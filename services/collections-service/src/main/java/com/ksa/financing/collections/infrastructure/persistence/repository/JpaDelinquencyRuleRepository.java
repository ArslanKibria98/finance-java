package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.DelinquencyRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDelinquencyRuleRepository extends JpaRepository<DelinquencyRuleJpaEntity, UUID> {

    Optional<DelinquencyRuleJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("""
            SELECT r FROM DelinquencyRuleJpaEntity r
             WHERE r.tenantId = :tenantId
               AND r.productId = :productId
               AND r.delinquencyType = :type
               AND r.recordState = 1
            """)
    Optional<DelinquencyRuleJpaEntity> findActive(@Param("tenantId") UUID tenantId,
                                                  @Param("productId") UUID productId,
                                                  @Param("type") short type);

    List<DelinquencyRuleJpaEntity> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);
}
