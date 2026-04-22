package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.LoanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLoanRepository extends JpaRepository<LoanJpaEntity, UUID> {

    Optional<LoanJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<LoanJpaEntity> findByTenantIdAndLoanNumber(UUID tenantId, String loanNumber);

    List<LoanJpaEntity> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<LoanJpaEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    Optional<LoanJpaEntity> findByTenantIdAndApplicationId(UUID tenantId, UUID applicationId);

    boolean existsByProductId(UUID productId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(l.loanNumber, 5) AS int)), 0) + 1 " +
           "FROM LoanJpaEntity l WHERE l.tenantId = :tenantId")
    int getNextLoanSequence(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT * FROM loans " +
                   "WHERE tenant_id = :tenantId " +
                   "AND UPPER(LEFT(CAST(id AS TEXT), 8)) = UPPER(:prefix) " +
                   "LIMIT 1",
           nativeQuery = true)
    Optional<LoanJpaEntity> findFirstByTenantIdAndIdPrefix(
            @Param("tenantId") UUID tenantId,
            @Param("prefix") String prefix);
}
