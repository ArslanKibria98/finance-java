package com.ksa.financing.piivault.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code pii_access_audits} table.
 * <p>
 * This is an IMMUTABLE (WORM) table — only INSERT operations are allowed.
 * The DB has triggers that prevent UPDATE and DELETE operations for compliance.
 * <p>
 * Each record is chained to the previous via {@code previousLogHash} and
 * {@code currentLogHash} using SHA-256 for tamper detection. The hash chaining
 * is computed by the DB trigger {@code chain_audit_log} on INSERT.
 * <p>
 * The {@code accessedFields} column is stored as a PostgreSQL VARCHAR(100)[] array,
 * mapped here as a comma-separated String (parsed in the mapper layer).
 */
@Entity
@Table(name = "pii_access_audits")
@Getter
@Setter
public class PiiAccessAuditJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;

    @Column(name = "pii_individual_id")
    private UUID piiIndividualId;

    @Column(name = "pii_business_id")
    private UUID piiBusinessId;

    @Column(name = "global_uid")
    private UUID globalUid;

    @Column(name = "accessor_id", nullable = false)
    private UUID accessorId;

    @Column(name = "accessor_role", nullable = false, length = 50)
    private String accessorRole;

    @Column(name = "accessor_ip", nullable = false, length = 45)
    private String accessorIp;

    @Column(name = "accessor_device_id", length = 100)
    private String accessorDeviceId;

    @Column(name = "accessor_session_id", length = 100)
    private String accessorSessionId;

    @Column(name = "accessed_fields", nullable = false, columnDefinition = "VARCHAR(100)[]")
    private String accessedFields;

    @Column(name = "access_operation", nullable = false, length = 20)
    private String accessOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_purpose", nullable = false, columnDefinition = "access_purpose")
    private AccessPurposeEnum accessPurpose;

    @Column(name = "access_justification", columnDefinition = "TEXT")
    private String accessJustification;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "access_token_id")
    private UUID accessTokenId;

    @Column(name = "access_token_expiry")
    private OffsetDateTime accessTokenExpiry;

    @Column(name = "access_granted", nullable = false)
    private boolean accessGranted;

    @Column(name = "denial_reason", length = 500)
    private String denialReason;

    @Column(name = "retrieved_data_hash", length = 64)
    private String retrievedDataHash;

    @Column(name = "previous_log_hash", length = 64)
    private String previousLogHash;

    @Column(name = "current_log_hash", nullable = false, length = 64)
    private String currentLogHash;

    @Column(name = "accessed_at", nullable = false)
    private OffsetDateTime accessedAt;

    /**
     * Enum for access_purpose PostgreSQL enum.
     */
    public enum AccessPurposeEnum {
        ONBOARDING,
        LOAN_APPLICATION_REVIEW,
        COMPLIANCE_CHECK,
        AML_SCREENING,
        CUSTOMER_SERVICE,
        REGULATORY_REPORT,
        AUDIT,
        DATA_EXPORT,
        DATA_DELETION
    }
}
