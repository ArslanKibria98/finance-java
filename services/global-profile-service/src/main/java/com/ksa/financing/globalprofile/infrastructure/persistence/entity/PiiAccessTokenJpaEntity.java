package com.ksa.financing.globalprofile.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code pii_access_tokens} table.
 * <p>
 * Time-limited tokens (max 10 minutes) for accessing PII vault data.
 * Tokens are hashed for storage and validated via the hash.
 * <p>
 * The {@code allowed_fields} column is stored as a PostgreSQL TEXT array
 * but mapped here as a comma-separated String for simplicity (parsed in mapper).
 */
@Entity
@Table(name = "pii_access_tokens")
@Getter
@Setter
public class PiiAccessTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id", updatable = false, nullable = false)
    private UUID tokenId;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "global_uid", nullable = false)
    private UUID globalUid;

    @Column(name = "regional_profile_id")
    private UUID regionalProfileId;

    @Column(name = "pii_vault_region", nullable = false, length = 10)
    private String piiVaultRegion;

    @Column(name = "allowed_fields", nullable = false, columnDefinition = "VARCHAR(100)[]")
    private String allowedFields;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "requester_role", nullable = false, length = 50)
    private String requesterRole;

    @Column(name = "requester_ip", length = 45)
    private String requesterIp;

    @Column(name = "access_purpose", nullable = false, length = 100)
    private String accessPurpose;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;
}
