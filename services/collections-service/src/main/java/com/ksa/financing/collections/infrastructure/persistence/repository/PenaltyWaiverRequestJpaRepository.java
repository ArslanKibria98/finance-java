package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.PenaltyWaiverRequestJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PenaltyWaiverRequestJpaRepository extends JpaRepository<PenaltyWaiverRequestJpaEntity, UUID> {
    List<PenaltyWaiverRequestJpaEntity> findAllByTenantIdAndLoanId(UUID tenantId, UUID loanId);
    List<PenaltyWaiverRequestJpaEntity> findAllByTenantIdAndApplicationId(UUID tenantId, UUID applicationId);
    List<PenaltyWaiverRequestJpaEntity> findAllByTenantIdAndInvoiceId(UUID tenantId, String invoiceId);
    List<PenaltyWaiverRequestJpaEntity> findAllByTenantIdAndRequestedBy(UUID tenantId, UUID requestedBy);
    List<PenaltyWaiverRequestJpaEntity> findAllByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantIdAndLoanIdAndStatus(UUID tenantId, UUID loanId, String status);

    Page<PenaltyWaiverRequestJpaEntity> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<PenaltyWaiverRequestJpaEntity> findAllByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);
}
