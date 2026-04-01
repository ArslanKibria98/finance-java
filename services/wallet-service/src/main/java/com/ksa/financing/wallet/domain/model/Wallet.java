package com.ksa.financing.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Wallet {
    private UUID id;
    private UUID tenantId;
    private String walletNumber;
    private UUID customerId;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
    private BigDecimal totalBalance;
    private String currency;
    private WalletStatus status;
    private BigDecimal dailyTopUpLimit;
    private BigDecimal monthlyTopUpLimit;
    private BigDecimal singleTopUpLimit;
    private BigDecimal todayTopUpAmount;
    private BigDecimal monthTopUpAmount;
    private Long fineractSavingsAccountId;
    private boolean ledgerSynced;
    private Instant lastLedgerSyncAt;
    private String iban;
    private boolean autoDebitEnabled;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getWalletNumber() { return walletNumber; }
    public void setWalletNumber(String walletNumber) { this.walletNumber = walletNumber; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
    public BigDecimal getTotalBalance() { return totalBalance; }
    public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; }
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
    public Long getFineractSavingsAccountId() { return fineractSavingsAccountId; }
    public void setFineractSavingsAccountId(Long fineractSavingsAccountId) { this.fineractSavingsAccountId = fineractSavingsAccountId; }
    public boolean isLedgerSynced() { return ledgerSynced; }
    public void setLedgerSynced(boolean ledgerSynced) { this.ledgerSynced = ledgerSynced; }
    public Instant getLastLedgerSyncAt() { return lastLedgerSyncAt; }
    public void setLastLedgerSyncAt(Instant lastLedgerSyncAt) { this.lastLedgerSyncAt = lastLedgerSyncAt; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public boolean isAutoDebitEnabled() { return autoDebitEnabled; }
    public void setAutoDebitEnabled(boolean autoDebitEnabled) { this.autoDebitEnabled = autoDebitEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
