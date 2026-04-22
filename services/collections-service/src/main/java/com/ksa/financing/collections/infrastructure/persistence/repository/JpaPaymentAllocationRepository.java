package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaPaymentAllocationRepository extends JpaRepository<PaymentAllocationJpaEntity, UUID> {

    @Query("SELECT a FROM PaymentAllocationJpaEntity a WHERE a.tenantId = :tenantId AND a.paymentId = :paymentId ORDER BY a.allocationOrder")
    List<PaymentAllocationJpaEntity> findByTenantIdAndPaymentId(
            @Param("tenantId") UUID tenantId, @Param("paymentId") UUID paymentId);
}
