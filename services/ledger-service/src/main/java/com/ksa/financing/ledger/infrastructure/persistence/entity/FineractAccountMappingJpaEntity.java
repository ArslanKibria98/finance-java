package com.ksa.financing.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for `fineract_account_mappings` table.
 * Bridge: Internal GL UUID ↔ Fineract GL ID.
 */
@Entity
@Table(
        name = "fineract_account_mappings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_internal_account", columnNames = {"tenant_id", "internal_account_id"}),
                @UniqueConstraint(name = "uq_fineract_account", columnNames = {"tenant_id", "fineract_gl_account_id"})
        },
        indexes = {
                @Index(name = "idx_fineract_acct_internal", columnList = "internal_account_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineractAccountMappingJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "internal_account_id", nullable = false)
    private UUID internalAccountId;

    @Column(name = "internal_account_code", nullable = false, length = 50)
    private String internalAccountCode;

    @Column(name = "fineract_gl_account_id", nullable = false)
    private Long fineractGlAccountId;

    @Column(name = "fineract_gl_account_code", nullable = false, length = 50)
    private String fineractGlAccountCode;

    @Column(name = "fineract_office_id", nullable = false)
    private Long fineractOfficeId;

    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "sync_status", nullable = false, length = 30)
    private String syncStatus;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
