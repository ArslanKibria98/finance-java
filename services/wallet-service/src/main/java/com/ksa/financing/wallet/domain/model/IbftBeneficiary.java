package com.ksa.financing.wallet.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * IBFT beneficiary — an external Canadian bank account a customer can send money to.
 * Canadian format: institution (FI) number + branch transit + account number.
 */
public class IbftBeneficiary {
    private UUID id;
    private UUID tenantId;
    private UUID customerId;
    private UUID walletId;
    private String nickname;
    private String beneficiaryName;
    private String institutionNumber;
    private String transit;
    private String accountNumber;
    private String bankName;
    private String currency;
    private boolean validated;
    private String validationRef;
    private String validationResult;   // raw JSON of the Scotia account-validation response
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getInstitutionNumber() { return institutionNumber; }
    public void setInstitutionNumber(String institutionNumber) { this.institutionNumber = institutionNumber; }
    public String getTransit() { return transit; }
    public void setTransit(String transit) { this.transit = transit; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }
    public String getValidationRef() { return validationRef; }
    public void setValidationRef(String validationRef) { this.validationRef = validationRef; }
    public String getValidationResult() { return validationResult; }
    public void setValidationResult(String validationResult) { this.validationResult = validationResult; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
