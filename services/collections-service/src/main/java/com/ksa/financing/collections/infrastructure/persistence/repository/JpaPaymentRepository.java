package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<PaymentJpaEntity> findByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.tenantId = :tenantId AND p.idempotencyKey = :key")
    Optional<PaymentJpaEntity> findByTenantIdAndIdempotencyKey(
            @Param("tenantId") UUID tenantId, @Param("key") String idempotencyKey);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.tenantId = :tenantId AND p.invoiceId = :invoiceId")
    Optional<PaymentJpaEntity> findByTenantIdAndInvoiceId(
            @Param("tenantId") UUID tenantId, @Param("invoiceId") String invoiceId);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.tenantId = :tenantId AND p.loanId = :loanId ORDER BY p.createdAt DESC")
    List<PaymentJpaEntity> findByTenantIdAndLoanId(
            @Param("tenantId") UUID tenantId, @Param("loanId") UUID loanId);
}
