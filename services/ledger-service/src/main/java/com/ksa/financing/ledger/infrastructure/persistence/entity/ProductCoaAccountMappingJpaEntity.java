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

@Entity
@Table(
        name = "product_coa_account_mappings",
        indexes = {
                @Index(name = "idx_product_acct_tenant", columnList = "tenant_id"),
                @Index(name = "idx_product_acct_product", columnList = "tenant_id, product_id"),
                @Index(name = "idx_product_acct_field", columnList = "tenant_id, coa_field_id"),
                @Index(name = "idx_product_acct_account", columnList = "tenant_id, account_id"),
                @Index(name = "idx_product_acct_status", columnList = "tenant_id, status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCoaAccountMappingJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "coa_field_id", nullable = false)
    private UUID coaFieldId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Version
    @Column(name = "version")
    private int version;
}
