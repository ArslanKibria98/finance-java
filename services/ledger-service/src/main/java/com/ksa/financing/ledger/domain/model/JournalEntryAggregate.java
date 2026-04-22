package com.ksa.financing.ledger.domain.model;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Journal Entry Aggregate Root.
 *
 * Core invariant: sum(debit lines) == sum(credit lines) — guaranteed by domain.
 * All state transitions emit domain events.
 * Pure domain class — zero Spring/JPA/Kafka imports.
 */
public class JournalEntryAggregate extends AggregateRoot<JournalEntryId> {

    private final JournalEntryId id;
    private final UUID tenantId;
    private final String entryNumber;
    private final String referenceType;
    private final UUID referenceId;
    private final String transactionType;
    private final LocalDate entryDate;
    private final LocalDate valueDate;
    private final String currency;
    private final String description;
    private final List<JournalLine> lines;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private EntryStatus status;
    private final boolean isReversal;
    private final JournalEntryId originalEntryId;
    private JournalEntryId reversedByEntryId;
    private String reversalReason;
    private boolean fineractSynced;
    private Long fineractTransactionId;
    private final UUID createdBy;
    private final LocalDateTime createdAt;

    private JournalEntryAggregate(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.entryNumber = builder.entryNumber;
        this.referenceType = builder.referenceType;
        this.referenceId = builder.referenceId;
        this.transactionType = builder.transactionType;
        this.entryDate = builder.entryDate;
        this.valueDate = builder.valueDate;
        this.currency = builder.currency;
        this.description = builder.description;
        this.lines = new ArrayList<>(builder.lines);
        this.totalDebit = builder.totalDebit;
        this.totalCredit = builder.totalCredit;
        this.status = builder.status;
        this.isReversal = builder.isReversal;
        this.originalEntryId = builder.originalEntryId;
        this.reversedByEntryId = builder.reversedByEntryId;
        this.reversalReason = builder.reversalReason;
        this.fineractSynced = builder.fineractSynced;
        this.fineractTransactionId = builder.fineractTransactionId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
    }

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    /**
     * Create a new balanced journal entry.
     * Validates that sum(debits) == sum(credits) before creation.
     *
     * @throws IllegalArgumentException if entry is unbalanced or invalid
     */
    public static JournalEntryAggregate create(
            UUID tenantId,
            String entryNumber,
            String referenceType,
            UUID referenceId,
            String transactionType,
            LocalDate entryDate,
            String description,
            List<JournalLine> lines,
            UUID createdBy) {

        // Invariant: tenant must be present
        Objects.requireNonNull(tenantId, "TenantId cannot be null");
        if (entryNumber == null || entryNumber.isBlank()) {
            throw new IllegalArgumentException("Entry number cannot be blank");
        }
        Objects.requireNonNull(referenceType, "ReferenceType cannot be null");
        Objects.requireNonNull(referenceId, "ReferenceId cannot be null");
        Objects.requireNonNull(entryDate, "Entry date cannot be null");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException("Journal entry must have at least 2 lines");
        }

        // Core invariant: balanced entry (debits == credits)
        BigDecimal totalDebit = lines.stream()
                .map(JournalLine::debitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal totalCredit = lines.stream()
                .map(JournalLine::creditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        if (totalDebit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Journal entry must have at least one debit line");
        }
        if (totalCredit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Journal entry must have at least one credit line");
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    "Journal entry is unbalanced: debits=" + totalDebit +
                    " credits=" + totalCredit +
                    " (difference=" + totalDebit.subtract(totalCredit).abs() + ")");
        }

        var entry = new Builder()
                .id(JournalEntryId.generate())
                .tenantId(tenantId)
                .entryNumber(entryNumber)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .transactionType(transactionType != null ? transactionType : referenceType)
                .entryDate(entryDate)
                .valueDate(entryDate)
                .currency("SAR")
                .description(description)
                .lines(lines)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .status(EntryStatus.PENDING)
                .isReversal(false)
                .fineractSynced(false)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        entry.registerEvent(new JournalEntryPosted(
                entry.id.value(),
                tenantId,
                entryNumber,
                referenceType,
                referenceId,
                totalDebit,
                entryDate,
                createdBy
        ));

        return entry;
    }

    /**
     * Reconstruct aggregate from persistence (no events emitted).
     */
    public static JournalEntryAggregate reconstitute(Builder builder) {
        return new JournalEntryAggregate(builder);
    }

    // -------------------------------------------------------------------------
    // Business operations
    // -------------------------------------------------------------------------

    /**
     * Mark this entry as POSTED (validated and accepted).
     */
    public void markPosted() {
        if (status != EntryStatus.PENDING) {
            throw new IllegalStateException(
                    "Can only post a PENDING entry. Current status: " + status);
        }
        this.status = EntryStatus.POSTED;
    }

    /**
     * Mark this entry as FAILED.
     */
    public void markFailed() {
        if (status != EntryStatus.PENDING) {
            throw new IllegalStateException(
                    "Can only fail a PENDING entry. Current status: " + status);
        }
        this.status = EntryStatus.FAILED;
    }

    /**
     * Mark this entry as REVERSED by a reversal entry.
     */
    public void reverse(JournalEntryId reversalEntryId, String reason) {
        if (status != EntryStatus.POSTED) {
            throw new IllegalStateException(
                    "Can only reverse a POSTED entry. Current status: " + status);
        }
        this.status = EntryStatus.REVERSED;
        this.reversedByEntryId = reversalEntryId;
        this.reversalReason = reason;
    }

    /**
     * Record successful Fineract sync.
     */
    public void recordFineractSync(Long fineractTransactionId) {
        Objects.requireNonNull(fineractTransactionId, "Fineract transaction ID cannot be null");
        this.fineractSynced = true;
        this.fineractTransactionId = fineractTransactionId;

        registerEvent(new JournalEntrySynced(
                id.value(),
                tenantId,
                entryNumber,
                fineractTransactionId
        ));
    }

    // -------------------------------------------------------------------------
    // Getters (no Lombok in domain — pure Java)
    // -------------------------------------------------------------------------

    @Override
    public JournalEntryId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEntryNumber() { return entryNumber; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public String getTransactionType() { return transactionType; }
    public LocalDate getEntryDate() { return entryDate; }
    public LocalDate getValueDate() { return valueDate; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public List<JournalLine> getLines() { return Collections.unmodifiableList(lines); }
    public BigDecimal getTotalDebit() { return totalDebit; }
    public BigDecimal getTotalCredit() { return totalCredit; }
    public EntryStatus getStatus() { return status; }
    public boolean isReversal() { return isReversal; }
    public JournalEntryId getOriginalEntryId() { return originalEntryId; }
    public JournalEntryId getReversedByEntryId() { return reversedByEntryId; }
    public String getReversalReason() { return reversalReason; }
    public boolean isFineractSynced() { return fineractSynced; }
    public Long getFineractTransactionId() { return fineractTransactionId; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JournalEntryId id;
        private UUID tenantId;
        private String entryNumber;
        private String referenceType;
        private UUID referenceId;
        private String transactionType;
        private LocalDate entryDate;
        private LocalDate valueDate;
        private String currency = "SAR";
        private String description;
        private List<JournalLine> lines = new ArrayList<>();
        private BigDecimal totalDebit = BigDecimal.ZERO;
        private BigDecimal totalCredit = BigDecimal.ZERO;
        private EntryStatus status = EntryStatus.PENDING;
        private boolean isReversal = false;
        private JournalEntryId originalEntryId;
        private JournalEntryId reversedByEntryId;
        private String reversalReason;
        private boolean fineractSynced = false;
        private Long fineractTransactionId;
        private UUID createdBy;
        private LocalDateTime createdAt;

        public Builder id(JournalEntryId id) { this.id = id; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder entryNumber(String entryNumber) { this.entryNumber = entryNumber; return this; }
        public Builder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public Builder referenceId(UUID referenceId) { this.referenceId = referenceId; return this; }
        public Builder transactionType(String transactionType) { this.transactionType = transactionType; return this; }
        public Builder entryDate(LocalDate entryDate) { this.entryDate = entryDate; return this; }
        public Builder valueDate(LocalDate valueDate) { this.valueDate = valueDate; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder lines(List<JournalLine> lines) { this.lines = lines; return this; }
        public Builder totalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; return this; }
        public Builder totalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; return this; }
        public Builder status(EntryStatus status) { this.status = status; return this; }
        public Builder isReversal(boolean isReversal) { this.isReversal = isReversal; return this; }
        public Builder originalEntryId(JournalEntryId originalEntryId) { this.originalEntryId = originalEntryId; return this; }
        public Builder reversedByEntryId(JournalEntryId reversedByEntryId) { this.reversedByEntryId = reversedByEntryId; return this; }
        public Builder reversalReason(String reversalReason) { this.reversalReason = reversalReason; return this; }
        public Builder fineractSynced(boolean fineractSynced) { this.fineractSynced = fineractSynced; return this; }
        public Builder fineractTransactionId(Long fineractTransactionId) { this.fineractTransactionId = fineractTransactionId; return this; }
        public Builder createdBy(UUID createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public JournalEntryAggregate build() {
            return new JournalEntryAggregate(this);
        }
    }

    // -------------------------------------------------------------------------
    // Domain Events (records — zero framework imports)
    // -------------------------------------------------------------------------

    public record JournalEntryPosted(
            UUID journalEntryId,
            UUID tenantId,
            String entryNumber,
            String referenceType,
            UUID referenceId,
            BigDecimal totalDebit,
            LocalDate entryDate,
            UUID createdBy
    ) {}

    public record JournalEntrySynced(
            UUID journalEntryId,
            UUID tenantId,
            String entryNumber,
            Long fineractTransactionId
    ) {}

    public record SyncFailed(
            UUID journalEntryId,
            UUID tenantId,
            String entryNumber,
            String errorCode,
            String errorMessage
    ) {}

    public record ReconciliationDiscrepancy(
            UUID accountId,
            UUID tenantId,
            java.time.LocalDate reconciliationDate,
            BigDecimal internalBalance,
            BigDecimal fineractBalance,
            BigDecimal variance
    ) {}
}
