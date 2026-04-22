package com.ksa.financing.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for `account_balances` table.
 * Daily snapshot of account debit/credit movements.
 */
@Entity
@Table(
        name = "account_balances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_account_balance_date", columnNames = {"account_id", "balance_date"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    @Column(name = "opening_balance", nullable = false, precision = 20, scale = 6)
    private BigDecimal openingBalance;

    @Column(name = "debit_movement", nullable = false, precision = 20, scale = 6)
    private BigDecimal debitMovement;

    @Column(name = "credit_movement", nullable = false, precision = 20, scale = 6)
    private BigDecimal creditMovement;

    @Column(name = "closing_balance", nullable = false, precision = 20, scale = 6)
    private BigDecimal closingBalance;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
