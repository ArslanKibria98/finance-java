package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_movements")
// NOTE: Filename should be WalletMovementJpaEntity.java - renamed class per naming conventions
public class WalletMovementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "movement_number", nullable = false, length = 50)
    private String movementNumber;

    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType;

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @Column(name = "amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 20, scale = 6)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 6)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "ledger_sync_status", nullable = false, length = 20)
    private String ledgerSyncStatus = "PENDING";

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "ledger_synced_at")
    private OffsetDateTime ledgerSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }

    public String getMovementNumber() { return movementNumber; }
    public void setMovementNumber(String movementNumber) { this.movementNumber = movementNumber; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getLedgerSyncStatus() { return ledgerSyncStatus; }
    public void setLedgerSyncStatus(String ledgerSyncStatus) { this.ledgerSyncStatus = ledgerSyncStatus; }

    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }

    public OffsetDateTime getLedgerSyncedAt() { return ledgerSyncedAt; }
    public void setLedgerSyncedAt(OffsetDateTime ledgerSyncedAt) { this.ledgerSyncedAt = ledgerSyncedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
