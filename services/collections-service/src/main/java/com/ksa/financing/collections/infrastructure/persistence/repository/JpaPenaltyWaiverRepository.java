package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.PenaltyWaiverJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPenaltyWaiverRepository extends JpaRepository<PenaltyWaiverJpaEntity, UUID> {

    Optional<PenaltyWaiverJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<PenaltyWaiverJpaEntity> findByTenantIdAndLoanIdOrderByWaivedAtDesc(UUID tenantId, UUID loanId);

    List<PenaltyWaiverJpaEntity> findByTenantIdAndInstallmentIdOrderByWaivedAtDesc(UUID tenantId, UUID installmentId);

    @Query("SELECT w FROM PenaltyWaiverJpaEntity w WHERE w.tenantId = :tenantId "
            + "AND w.waivedAt >= :from AND w.waivedAt < :to ORDER BY w.waivedAt DESC")
    List<PenaltyWaiverJpaEntity> findByDateRange(@Param("tenantId") UUID tenantId,
                                                 @Param("from") java.time.LocalDateTime from,
                                                 @Param("to") java.time.LocalDateTime to);

    List<PenaltyWaiverJpaEntity> findByTenantIdOrderByWaivedAtDesc(UUID tenantId, Pageable pageable);
}
