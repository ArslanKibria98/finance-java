package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
// NOTE: Filename should be WalletJpaEntity.java - renamed class per naming conventions
public class WalletJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "wallet_number", nullable = false, length = 50)
    private String walletNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Column(name = "available_balance", nullable = false, precision = 20, scale = 6)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "reserved_balance", nullable = false, precision = 20, scale = 6)
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    // total_balance is GENERATED ALWAYS AS (available_balance + reserved_balance) STORED
    // Hibernate cannot write to it; read-only column
    @Column(name = "total_balance", insertable = false, updatable = false, precision = 20, scale = 6)
    private BigDecimal totalBalance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "SAR";

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "daily_top_up_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal dailyTopUpLimit;

    @Column(name = "monthly_top_up_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal monthlyTopUpLimit;

    @Column(name = "single_top_up_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal singleTopUpLimit;

    @Column(name = "today_top_up_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal todayTopUpAmount = BigDecimal.ZERO;

    @Column(name = "month_top_up_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal monthTopUpAmount = BigDecimal.ZERO;

    @Column(name = "single_transaction_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal singleTransactionLimit;

    @Column(name = "daily_transaction_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal dailyTransactionLimit;

    @Column(name = "monthly_transaction_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal monthlyTransactionLimit;

    @Column(name = "yearly_transaction_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal yearlyTransactionLimit;

    @Column(name = "fineract_savings_account_id")
    private Long fineractSavingsAccountId;

    @Column(name = "ledger_synced", nullable = false)
    private boolean ledgerSynced = false;

    @Column(name = "last_ledger_sync_at")
    private OffsetDateTime lastLedgerSyncAt;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "masked_name", length = 120)
    private String maskedName;

    @Column(name = "english_first_name", length = 60)
    private String englishFirstName;

    @Column(name = "english_third_name", length = 60)
    private String englishThirdName;

    @Column(name = "auto_debit_enabled", nullable = false)
    private boolean autoDebitEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getWalletNumber() { return walletNumber; }
    public void setWalletNumber(String walletNumber) { this.walletNumber = walletNumber; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }

    public BigDecimal getTotalBalance() { return totalBalance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getDailyTopUpLimit() { return dailyTopUpLimit; }
    public void setDailyTopUpLimit(BigDecimal dailyTopUpLimit) { this.dailyTopUpLimit = dailyTopUpLimit; }

    public BigDecimal getMonthlyTopUpLimit() { return monthlyTopUpLimit; }
    public void setMonthlyTopUpLimit(BigDecimal monthlyTopUpLimit) { this.monthlyTopUpLimit = monthlyTopUpLimit; }

    public BigDecimal getSingleTopUpLimit() { return singleTopUpLimit; }
    public void setSingleTopUpLimit(BigDecimal singleTopUpLimit) { this.singleTopUpLimit = singleTopUpLimit; }

    public BigDecimal getTodayTopUpAmount() { return todayTopUpAmount; }
    public void setTodayTopUpAmount(BigDecimal todayTopUpAmount) { this.todayTopUpAmount = todayTopUpAmount; }

    public BigDecimal getMonthTopUpAmount() { return monthTopUpAmount; }
    public void setMonthTopUpAmount(BigDecimal monthTopUpAmount) { this.monthTopUpAmount = monthTopUpAmount; }

    public BigDecimal getSingleTransactionLimit() { return singleTransactionLimit; }
    public void setSingleTransactionLimit(BigDecimal singleTransactionLimit) { this.singleTransactionLimit = singleTransactionLimit; }

    public BigDecimal getDailyTransactionLimit() { return dailyTransactionLimit; }
    public void setDailyTransactionLimit(BigDecimal dailyTransactionLimit) { this.dailyTransactionLimit = dailyTransactionLimit; }

    public BigDecimal getMonthlyTransactionLimit() { return monthlyTransactionLimit; }
    public void setMonthlyTransactionLimit(BigDecimal monthlyTransactionLimit) { this.monthlyTransactionLimit = monthlyTransactionLimit; }

    public BigDecimal getYearlyTransactionLimit() { return yearlyTransactionLimit; }
    public void setYearlyTransactionLimit(BigDecimal yearlyTransactionLimit) { this.yearlyTransactionLimit = yearlyTransactionLimit; }

    public Long getFineractSavingsAccountId() { return fineractSavingsAccountId; }
    public void setFineractSavingsAccountId(Long fineractSavingsAccountId) { this.fineractSavingsAccountId = fineractSavingsAccountId; }

    public boolean isLedgerSynced() { return ledgerSynced; }
    public void setLedgerSynced(boolean ledgerSynced) { this.ledgerSynced = ledgerSynced; }

    public OffsetDateTime getLastLedgerSyncAt() { return lastLedgerSyncAt; }
    public void setLastLedgerSyncAt(OffsetDateTime lastLedgerSyncAt) { this.lastLedgerSyncAt = lastLedgerSyncAt; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getMaskedName() { return maskedName; }
    public void setMaskedName(String maskedName) { this.maskedName = maskedName; }

    public String getEnglishFirstName() { return englishFirstName; }
    public void setEnglishFirstName(String englishFirstName) { this.englishFirstName = englishFirstName; }

    public String getEnglishThirdName() { return englishThirdName; }
    public void setEnglishThirdName(String englishThirdName) { this.englishThirdName = englishThirdName; }

    public boolean isAutoDebitEnabled() { return autoDebitEnabled; }
    public void setAutoDebitEnabled(boolean autoDebitEnabled) { this.autoDebitEnabled = autoDebitEnabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
