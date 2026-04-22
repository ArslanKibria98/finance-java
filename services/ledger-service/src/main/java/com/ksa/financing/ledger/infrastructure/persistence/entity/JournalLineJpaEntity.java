package com.ksa.financing.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the `journal_lines` table.
 * Child of JournalEntryJpaEntity.
 */
@Entity
@Table(
        name = "journal_lines",
        indexes = {
                @Index(name = "idx_lines_entry", columnList = "journal_entry_id"),
                @Index(name = "idx_lines_account", columnList = "account_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_entry_line", columnNames = {"journal_entry_id", "line_number"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLineJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryJpaEntity journalEntry;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "debit_amount", precision = 20, scale = 6)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", precision = 20, scale = 6)
    private BigDecimal creditAmount;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sub_ledger_type", length = 50)
    private String subLedgerType;

    @Column(name = "sub_ledger_id")
    private UUID subLedgerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
