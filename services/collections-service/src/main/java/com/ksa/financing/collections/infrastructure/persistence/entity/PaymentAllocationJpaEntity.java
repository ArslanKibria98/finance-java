package com.ksa.financing.collections.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PaymentAllocationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "installment_id", nullable = false)
    private UUID installmentId;

    @Column(name = "allocation_order", nullable = false)
    private int allocationOrder;

    @Column(name = "principal_allocated", nullable = false, precision = 20, scale = 6)
    private BigDecimal principalAllocated;

    @Column(name = "profit_allocated", nullable = false, precision = 20, scale = 6)
    private BigDecimal profitAllocated;

    @Column(name = "fee_allocated", nullable = false, precision = 20, scale = 6)
    private BigDecimal feeAllocated;

    @Column(name = "total_allocated", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalAllocated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
