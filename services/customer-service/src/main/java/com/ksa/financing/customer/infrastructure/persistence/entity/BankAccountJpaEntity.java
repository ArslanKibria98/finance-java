package com.ksa.financing.customer.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
// NOTE: Filename should be BankAccountJpaEntity.java - renamed class per naming conventions
public class BankAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "iban", nullable = false, length = 255)
    private String iban;

    @Column(name = "account_holder_name", length = 255)
    private String accountHolderName;

    @Column(name = "account_type", length = 50)
    private String accountType;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "is_salary_account", nullable = false)
    private boolean salaryAccount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "verification_method", length = 50)
    private String verificationMethod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
