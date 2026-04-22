package com.ksa.financing.ledger.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * GL Account Aggregate Root (Chart of Accounts entry).
 *
 * Represents an account in the Chart of Accounts (COA).
 * Manages account lifecycle: ACTIVE → INACTIVE / SUSPENDED / CLOSED.
 * Pure domain class — zero Spring/JPA/Kafka imports.
 */
public class AccountAggregate extends AggregateRoot<AccountId> {

    private final AccountId id;
    private final UUID tenantId;
    private final String accountCode;
    private String accountName;
    private String accountNameAr;
    private final AccountType accountType;
    private final AccountId parentAccountId;
    private final int hierarchyLevel;
    private String hierarchyPath;
    private final boolean isHeader;
    private boolean isManualEntriesAllowed;
    private AccountStatus status;
    private UUID fineractMappingId;
    private String iban;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AccountAggregate(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.accountCode = builder.accountCode;
        this.accountName = builder.accountName;
        this.accountNameAr = builder.accountNameAr;
        this.accountType = builder.accountType;
        this.parentAccountId = builder.parentAccountId;
        this.hierarchyLevel = builder.hierarchyLevel;
        this.hierarchyPath = builder.hierarchyPath;
        this.isHeader = builder.isHeader;
        this.isManualEntriesAllowed = builder.isManualEntriesAllowed;
        this.status = builder.status;
        this.fineractMappingId = builder.fineractMappingId;
        this.iban = builder.iban;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    public static AccountAggregate create(
            UUID tenantId,
            String accountCode,
            String accountName,
            AccountType accountType,
            AccountId parentAccountId,
            boolean isHeader) {

        Objects.requireNonNull(tenantId, "TenantId cannot be null");
        if (accountCode == null || accountCode.isBlank()) {
            throw new IllegalArgumentException("Account code cannot be blank");
        }
        if (accountName == null || accountName.isBlank()) {
            throw new IllegalArgumentException("Account name cannot be blank");
        }
        Objects.requireNonNull(accountType, "AccountType cannot be null");

        var account = new Builder()
                .id(AccountId.generate())
                .tenantId(tenantId)
                .accountCode(accountCode.toUpperCase().trim())
                .accountName(accountName)
                .accountType(accountType)
                .parentAccountId(parentAccountId)
                .hierarchyLevel(parentAccountId == null ? 0 : 1)
                .isHeader(isHeader)
                .isManualEntriesAllowed(!isHeader)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        account.registerEvent(new AccountCreated(
                account.id.value(),
                tenantId,
                accountCode,
                accountType
        ));

        return account;
    }

    public static AccountAggregate reconstitute(Builder builder) {
        return new AccountAggregate(builder);
    }

    // -------------------------------------------------------------------------
    // Business operations
    // -------------------------------------------------------------------------

    public void updateName(String newName, String newNameAr) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Account name cannot be blank");
        }
        this.accountName = newName;
        this.accountNameAr = newNameAr;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot deactivate a CLOSED account");
        }
        this.status = AccountStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot activate a CLOSED account");
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already CLOSED");
        }
        this.status = AccountStatus.CLOSED;
        this.isManualEntriesAllowed = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void linkToFineract(UUID mappingId) {
        Objects.requireNonNull(mappingId, "Fineract mapping ID cannot be null");
        this.fineractMappingId = mappingId;
        this.updatedAt = LocalDateTime.now();
    }

    public void setIban(String iban) {
        this.iban = iban;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean allowsManualEntries() {
        return isManualEntriesAllowed && status == AccountStatus.ACTIVE;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @Override
    public AccountId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getAccountCode() { return accountCode; }
    public String getAccountName() { return accountName; }
    public String getAccountNameAr() { return accountNameAr; }
    public AccountType getAccountType() { return accountType; }
    public AccountId getParentAccountId() { return parentAccountId; }
    public int getHierarchyLevel() { return hierarchyLevel; }
    public String getHierarchyPath() { return hierarchyPath; }
    public boolean isHeader() { return isHeader; }
    public boolean isManualEntriesAllowed() { return isManualEntriesAllowed; }
    public AccountStatus getStatus() { return status; }
    public UUID getFineractMappingId() { return fineractMappingId; }
    public String getIban() { return iban; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private AccountId id;
        private UUID tenantId;
        private String accountCode;
        private String accountName;
        private String accountNameAr;
        private AccountType accountType;
        private AccountId parentAccountId;
        private int hierarchyLevel;
        private String hierarchyPath;
        private boolean isHeader;
        private boolean isManualEntriesAllowed = true;
        private AccountStatus status = AccountStatus.ACTIVE;
        private UUID fineractMappingId;
        private String iban;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(AccountId id) { this.id = id; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder accountCode(String accountCode) { this.accountCode = accountCode; return this; }
        public Builder accountName(String accountName) { this.accountName = accountName; return this; }
        public Builder accountNameAr(String accountNameAr) { this.accountNameAr = accountNameAr; return this; }
        public Builder accountType(AccountType accountType) { this.accountType = accountType; return this; }
        public Builder parentAccountId(AccountId parentAccountId) { this.parentAccountId = parentAccountId; return this; }
        public Builder hierarchyLevel(int hierarchyLevel) { this.hierarchyLevel = hierarchyLevel; return this; }
        public Builder hierarchyPath(String hierarchyPath) { this.hierarchyPath = hierarchyPath; return this; }
        public Builder isHeader(boolean isHeader) { this.isHeader = isHeader; return this; }
        public Builder isManualEntriesAllowed(boolean isManualEntriesAllowed) { this.isManualEntriesAllowed = isManualEntriesAllowed; return this; }
        public Builder status(AccountStatus status) { this.status = status; return this; }
        public Builder fineractMappingId(UUID fineractMappingId) { this.fineractMappingId = fineractMappingId; return this; }
        public Builder iban(String iban) { this.iban = iban; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AccountAggregate build() { return new AccountAggregate(this); }
    }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    public record AccountCreated(
            UUID accountId,
            UUID tenantId,
            String accountCode,
            AccountType accountType
    ) {}
}
