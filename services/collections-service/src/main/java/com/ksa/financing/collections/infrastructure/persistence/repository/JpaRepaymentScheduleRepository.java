package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.RepaymentScheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaRepaymentScheduleRepository extends JpaRepository<RepaymentScheduleJpaEntity, UUID> {

    @Query("SELECT r FROM RepaymentScheduleJpaEntity r WHERE r.tenantId = :tenantId AND r.id = :id")
    Optional<RepaymentScheduleJpaEntity> findByTenantIdAndId(
            @Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Query("SELECT r FROM RepaymentScheduleJpaEntity r WHERE r.tenantId = :tenantId AND r.loanId = :loanId AND r.active = true")
    Optional<RepaymentScheduleJpaEntity> findActiveByTenantIdAndLoanId(
            @Param("tenantId") UUID tenantId, @Param("loanId") UUID loanId);

    boolean existsByTenantIdAndScheduleNumber(UUID tenantId, String scheduleNumber);
}
