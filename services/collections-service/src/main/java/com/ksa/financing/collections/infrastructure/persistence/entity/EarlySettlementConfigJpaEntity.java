package com.ksa.financing.collections.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "early_settlement_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EarlySettlementConfigJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "delinquency_id", nullable = false)
    private UUID delinquencyId;

    @Column(name = "invoice_order")
    private Integer invoiceOrder;

    @Column(name = "from_day", nullable = false)
    private int fromDay;

    @Column(name = "till_day", nullable = false)
    private int tillDay;

    @Column(name = "is_percentage", nullable = false)
    private boolean isPercentage;

    @Column(name = "discount_percentage", nullable = false, precision = 9, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "is_range", nullable = false)
    private boolean isRange;

    @Column(name = "range_no", nullable = false)
    private int rangeNo;

    @Column(name = "min_invoice_order")
    private Integer minInvoiceOrder;

    @Column(name = "max_invoice_order")
    private Integer maxInvoiceOrder;

    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @Column(name = "record_state", nullable = false)
    private short recordState;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
