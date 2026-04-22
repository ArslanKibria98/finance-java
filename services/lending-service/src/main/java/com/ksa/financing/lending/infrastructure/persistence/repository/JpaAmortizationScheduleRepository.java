package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.AmortizationScheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAmortizationScheduleRepository extends JpaRepository<AmortizationScheduleJpaEntity, UUID> {

    List<AmortizationScheduleJpaEntity> findByLoanIdAndActiveOrderByInstallmentNumberAsc(UUID loanId, boolean active);

    List<AmortizationScheduleJpaEntity> findByLoanIdAndActiveAndPaymentStatusOrderByInstallmentNumberAsc(
            UUID loanId, boolean active, String paymentStatus);

    @Modifying
    @Query("UPDATE AmortizationScheduleJpaEntity a SET a.active = false WHERE a.loanId = :loanId AND a.active = true")
    void deactivateAllByLoanId(@Param("loanId") UUID loanId);

    @Modifying
    @Query("UPDATE AmortizationScheduleJpaEntity a SET a.dueDate = :newDueDate WHERE a.id = :id")
    void updateDueDate(@Param("id") UUID id, @Param("newDueDate") LocalDate newDueDate);

    @Query("SELECT MAX(a.scheduleVersion) FROM AmortizationScheduleJpaEntity a WHERE a.loanId = :loanId")
    Integer findMaxScheduleVersionByLoanId(@Param("loanId") UUID loanId);
}
