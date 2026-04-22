package com.ksa.financing.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the `accounts` table (Chart of Accounts).
 * Separate from AccountAggregate domain model.
 */
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_tenant", columnList = "tenant_id"),
                @Index(name = "idx_accounts_type", columnList = "tenant_id, account_type"),
                @Index(name = "idx_accounts_parent", columnList = "parent_account_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_account_code", columnNames = {"tenant_id", "account_code"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(name = "account_name_ar", length = 255)
    private String accountNameAr;

    @Column(name = "account_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountTypeDb accountType;

    @Column(name = "parent_account_id")
    private UUID parentAccountId;

    @Column(name = "hierarchy_level", nullable = false)
    private int hierarchyLevel;

    @Column(name = "hierarchy_path", length = 500)
    private String hierarchyPath;

    @Column(name = "is_header", nullable = false)
    private boolean isHeader;

    @Column(name = "is_manual_entries_allowed", nullable = false)
    private boolean isManualEntriesAllowed;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountStatusDb status;

    @Column(name = "fineract_mapping_id")
    private UUID fineractMappingId;

    @Column(name = "iban", length = 34)
    private String iban;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    /**
     * String-based enum proxy to avoid Hibernate mapping issues with PostgreSQL custom types.
     */
    public enum AccountTypeDb {
        ASSET, LIABILITY, EQUITY, INCOME, EXPENSE, OFF_BALANCE
    }

    public enum AccountStatusDb {
        ACTIVE, INACTIVE, CLOSED, SUSPENDED
    }
}
