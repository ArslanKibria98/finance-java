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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the `journal_entries` table.
 * Separate from JournalEntryAggregate domain model.
 */
@Entity
@Table(
        name = "journal_entries",
        indexes = {
                @Index(name = "idx_entries_tenant", columnList = "tenant_id"),
                @Index(name = "idx_entries_date", columnList = "entry_date"),
                @Index(name = "idx_entries_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_entries_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_entry_number", columnNames = {"tenant_id", "entry_number"}),
                @UniqueConstraint(name = "uq_idempotency", columnNames = {"tenant_id", "idempotency_key"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "entry_number", nullable = false, length = 50)
    private String entryNumber;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_debit", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalCredit;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "is_reversal", nullable = false)
    private boolean isReversal;

    @Column(name = "original_entry_id")
    private UUID originalEntryId;

    @Column(name = "reversed_by_entry_id")
    private UUID reversedByEntryId;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "fineract_synced", nullable = false)
    private boolean fineractSynced;

    @Column(name = "fineract_transaction_id")
    private Long fineractTransactionId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @OneToMany(
            mappedBy = "journalEntry",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @Builder.Default
    private List<JournalLineJpaEntity> lines = new ArrayList<>();
}
