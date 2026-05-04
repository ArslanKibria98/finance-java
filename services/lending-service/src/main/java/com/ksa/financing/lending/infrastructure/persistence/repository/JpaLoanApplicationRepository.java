package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.LoanApplicationJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLoanApplicationRepository extends JpaRepository<LoanApplicationJpaEntity, UUID>, JpaSpecificationExecutor<LoanApplicationJpaEntity> {

    Optional<LoanApplicationJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<LoanApplicationJpaEntity> findByTenantIdAndApplicationNumber(UUID tenantId, String applicationNumber);

    Optional<LoanApplicationJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<LoanApplicationJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<LoanApplicationJpaEntity> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    List<LoanApplicationJpaEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    boolean existsByTenantIdAndApplicationNumber(UUID tenantId, String applicationNumber);

    boolean existsByProductIdAndStatusNotIn(UUID productId, List<String> statuses);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(la.applicationNumber, 5) AS int)), 0) + 1 " +
           "FROM LoanApplicationJpaEntity la WHERE la.tenantId = :tenantId")
    int getNextApplicationSequence(@Param("tenantId") UUID tenantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LoanApplicationJpaEntity la " +
           "SET la.status = :status, la.currentStage = :status, la.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE la.tenantId = :tenantId AND la.id = :applicationId")
    int updateApplicationStatus(@Param("tenantId") UUID tenantId,
                                @Param("applicationId") UUID applicationId,
                                @Param("status") String status);
}
