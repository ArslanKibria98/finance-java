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
@Table(name = "delinquency_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DelinquencyRuleJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "delinquency_type", nullable = false)
    private short delinquencyType;

    @Column(name = "is_percentage", nullable = false)
    private boolean isPercentage;

    @Column(name = "penalty_percentage", nullable = false, precision = 9, scale = 2)
    private BigDecimal penaltyPercentage;

    @Column(name = "penalty_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal penaltyAmount;

    @Column(name = "from_day", nullable = false)
    private int fromDay;

    @Column(name = "till_day", nullable = false)
    private int tillDay;

    @Column(name = "penalty_type", nullable = false)
    private short penaltyType;

    @Column(name = "promises_per_year", nullable = false)
    private int promisesPerYear;

    @Column(name = "promises_per_loan", nullable = false)
    private int promisesPerLoan;

    @Column(name = "is_custom", nullable = false)
    private boolean isCustom;

    @Column(name = "settlement_strategy", nullable = false)
    private short settlementStrategy;

    @Column(name = "settlement_discount_type", length = 20)
    private String settlementDiscountType;

    @Column(name = "settlement_months")
    private int settlementMonths;

    @Column(name = "settlement_amount_per_month", precision = 19, scale = 4)
    private BigDecimal settlementAmountPerMonth;

    @Column(name = "charity_fund_account", length = 100)
    private String charityFundAccount;

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

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
