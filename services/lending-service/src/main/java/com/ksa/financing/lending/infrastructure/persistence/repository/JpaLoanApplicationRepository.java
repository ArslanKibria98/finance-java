package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.LoanApplicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLoanApplicationRepository extends JpaRepository<LoanApplicationJpaEntity, UUID> {

    Optional<LoanApplicationJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<LoanApplicationJpaEntity> findByTenantIdAndApplicationNumber(UUID tenantId, String applicationNumber);

    Optional<LoanApplicationJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<LoanApplicationJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<LoanApplicationJpaEntity> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    List<LoanApplicationJpaEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    boolean existsByTenantIdAndApplicationNumber(UUID tenantId, String applicationNumber);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(la.applicationNumber, 5) AS int)), 0) + 1 " +
           "FROM LoanApplicationJpaEntity la WHERE la.tenantId = :tenantId")
    int getNextApplicationSequence(@Param("tenantId") UUID tenantId);
}
