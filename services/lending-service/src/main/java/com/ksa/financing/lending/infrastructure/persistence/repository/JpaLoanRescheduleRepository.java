package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.LoanRescheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLoanRescheduleRepository extends JpaRepository<LoanRescheduleJpaEntity, UUID> {

    List<LoanRescheduleJpaEntity> findByTenantIdAndLoanId(UUID tenantId, UUID loanId);

    List<LoanRescheduleJpaEntity> findByTenantIdAndLoanIdAndStatus(UUID tenantId, UUID loanId, String status);

    Optional<LoanRescheduleJpaEntity> findByWorkflowId(String workflowId);

    Optional<LoanRescheduleJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    boolean existsByTenantIdAndLoanIdAndRescheduleTypeAndStatusIn(
            UUID tenantId, UUID loanId, String rescheduleType, List<String> statuses);

    boolean existsByTenantIdAndLoanIdAndStatusIn(UUID tenantId, UUID loanId, List<String> statuses);

    List<LoanRescheduleJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<LoanRescheduleJpaEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);

    List<LoanRescheduleJpaEntity> findByTenantIdAndRescheduleTypeOrderByCreatedAtDesc(UUID tenantId, String rescheduleType);
}
