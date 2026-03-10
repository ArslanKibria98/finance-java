package com.ksa.financing.customer.domain.model;

import java.time.Instant;
import java.util.UUID;

public class BankAccount {
    private UUID id;
    private UUID tenantId;
    private UUID customerId;
    private String bankName;
    private String bankCode;
    private String iban;
    private String accountHolderName;
    private String accountType;
    private boolean primary;
    private boolean salaryAccount;
    private BankAccountStatus status;
    private Instant verifiedAt;
    private String verificationMethod;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public boolean isSalaryAccount() { return salaryAccount; }
    public void setSalaryAccount(boolean salaryAccount) { this.salaryAccount = salaryAccount; }
    public BankAccountStatus getStatus() { return status; }
    public void setStatus(BankAccountStatus status) { this.status = status; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
