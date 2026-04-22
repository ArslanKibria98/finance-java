package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.DunningPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDunningPolicyRepository extends JpaRepository<DunningPolicyJpaEntity, UUID> {

    Optional<DunningPolicyJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<DunningPolicyJpaEntity> findByTenantIdAndPolicyName(UUID tenantId, String policyName);

    @Query("""
            SELECT p FROM DunningPolicyJpaEntity p
             WHERE p.tenantId = :tenantId
               AND p.productCode = :productCode
               AND p.active = true
            """)
    Optional<DunningPolicyJpaEntity> findActiveByProductCode(@Param("tenantId") UUID tenantId,
                                                              @Param("productCode") String productCode);

    @Query("""
            SELECT p FROM DunningPolicyJpaEntity p
             WHERE p.tenantId = :tenantId
               AND p.productCode IS NULL
               AND p.defaultPolicy = true
               AND p.active = true
            """)
    Optional<DunningPolicyJpaEntity> findActiveDefault(@Param("tenantId") UUID tenantId);

    List<DunningPolicyJpaEntity> findAllByTenantId(UUID tenantId);

    List<DunningPolicyJpaEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);

    boolean existsByTenantIdAndPolicyName(UUID tenantId, String policyName);

    @Modifying
    @Query("""
            UPDATE DunningPolicyJpaEntity p
               SET p.defaultPolicy = false
             WHERE p.tenantId = :tenantId
               AND p.id <> :keepId
               AND p.defaultPolicy = true
            """)
    int clearDefaultExcept(@Param("tenantId") UUID tenantId, @Param("keepId") UUID keepId);

    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
