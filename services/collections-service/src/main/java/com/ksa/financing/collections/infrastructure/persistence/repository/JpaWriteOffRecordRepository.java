package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.WriteOffRecordJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWriteOffRecordRepository
        extends JpaRepository<WriteOffRecordJpaEntity, UUID>,
                JpaSpecificationExecutor<WriteOffRecordJpaEntity> {

    Optional<WriteOffRecordJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<WriteOffRecordJpaEntity> findByTenantIdAndLoanIdOrderByCreatedAtDesc(UUID tenantId, UUID loanId);

    List<WriteOffRecordJpaEntity> findByTenantIdAndInstallmentIdOrderByCreatedAtDesc(UUID tenantId, UUID installmentId);

    @Query("SELECT w FROM WriteOffRecordJpaEntity w WHERE w.tenantId = :tenantId "
            + "AND w.writeOffDate BETWEEN :from AND :to ORDER BY w.writeOffDate DESC")
    List<WriteOffRecordJpaEntity> findByDateRange(@Param("tenantId") UUID tenantId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    List<WriteOffRecordJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
