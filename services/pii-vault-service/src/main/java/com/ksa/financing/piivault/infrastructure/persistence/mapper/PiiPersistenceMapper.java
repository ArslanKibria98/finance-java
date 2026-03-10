package com.ksa.financing.piivault.infrastructure.persistence.mapper;

import com.ksa.financing.piivault.domain.model.AccessPurpose;
import com.ksa.financing.piivault.domain.model.PiiAccessAudit;
import com.ksa.financing.piivault.domain.model.PiiIndividual;
import com.ksa.financing.piivault.domain.model.VaultRegion;
import com.ksa.financing.piivault.infrastructure.persistence.entity.PiiAccessAuditJpaEntity;
import com.ksa.financing.piivault.infrastructure.persistence.entity.PiiIndividualJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between domain models and JPA entities for the PII Vault Service.
 * <p>
 * IMPORTANT: This mapper does NOT handle encryption/decryption of PII fields.
 * The entity stores encrypted byte[] data, while the domain model stores
 * plaintext strings. Encryption is handled by the {@code EncryptionPort}
 * in the repository implementation layer.
 * <p>
 * This mapper only handles:
 * <ul>
 *   <li>Non-encrypted field mapping (IDs, enums, metadata)</li>
 *   <li>Enum conversions between domain and entity layers</li>
 *   <li>Instant to OffsetDateTime conversions</li>
 *   <li>List to PostgreSQL array string conversions (for accessed_fields)</li>
 * </ul>
 */
@Component
public class PiiPersistenceMapper {

    // -----------------------------------------------------------------------
    // PiiIndividual ↔ PiiIndividualJpaEntity (non-encrypted fields only)
    // -----------------------------------------------------------------------

    /**
     * Maps domain model to entity for NON-ENCRYPTED fields only.
     * Encrypted BYTEA fields must be set separately by the repository
     * after calling EncryptionPort.encrypt().
     */
    public PiiIndividualJpaEntity toEntity(PiiIndividual domain) {
        if (domain == null) return null;

        PiiIndividualJpaEntity entity = new PiiIndividualJpaEntity();
        entity.setPiiId(domain.getPiiId());
        entity.setGlobalUid(domain.getGlobalUid());
        entity.setVaultRegion(toEntityVaultRegion(domain.getVaultRegion()));
        entity.setNationalIdType(domain.getNationalIdType());
        entity.setGender(domain.getGender());
        entity.setNationalityCode(domain.getNationalityCode());
        entity.setCountryCode(domain.getCountryCode());
        entity.setBankName(domain.getBankName());
        entity.setEncryptionKeyVersion(domain.getEncryptionKeyVersion());
        entity.setEncryptionAlgorithm("AES-256-GCM");
        entity.setEncryptedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    /**
     * Maps entity to domain model for NON-ENCRYPTED fields only.
     * Decrypted plaintext PII fields must be set separately by the repository
     * after calling EncryptionPort.decrypt().
     */
    public PiiIndividual toDomain(PiiIndividualJpaEntity entity) {
        if (entity == null) return null;

        PiiIndividual domain = new PiiIndividual();
        domain.setPiiId(entity.getPiiId());
        domain.setGlobalUid(entity.getGlobalUid());
        domain.setVaultRegion(toDomainVaultRegion(entity.getVaultRegion()));
        domain.setNationalIdType(entity.getNationalIdType());
        domain.setGender(entity.getGender());
        domain.setNationalityCode(entity.getNationalityCode());
        domain.setCountryCode(entity.getCountryCode());
        domain.setBankName(entity.getBankName());
        domain.setEncryptionKeyVersion(entity.getEncryptionKeyVersion());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // -----------------------------------------------------------------------
    // PiiAccessAudit ↔ PiiAccessAuditJpaEntity
    // -----------------------------------------------------------------------

    public PiiAccessAuditJpaEntity toEntity(PiiAccessAudit domain) {
        if (domain == null) return null;

        PiiAccessAuditJpaEntity entity = new PiiAccessAuditJpaEntity();
        entity.setAuditId(domain.getAuditId());
        entity.setPiiIndividualId(domain.getPiiIndividualId());
        entity.setPiiBusinessId(domain.getPiiBusinessId());
        entity.setGlobalUid(domain.getGlobalUid());
        entity.setAccessorId(domain.getAccessorId());
        entity.setAccessorRole(domain.getAccessorRole());
        entity.setAccessorIp(domain.getAccessorIp());
        entity.setAccessedFields(toPostgresArray(domain.getAccessedFields()));
        entity.setAccessOperation(domain.getAccessOperation());
        entity.setAccessPurpose(toEntityAccessPurpose(domain.getAccessPurpose()));
        entity.setAccessJustification(domain.getAccessJustification());
        entity.setAccessGranted(domain.isAccessGranted());
        entity.setDenialReason(domain.getDenialReason());
        entity.setAccessedAt(toOffsetDateTime(domain.getAccessedAt()));
        // currentLogHash and previousLogHash are computed by DB trigger — leave null
        entity.setCurrentLogHash("PLACEHOLDER");
        return entity;
    }

    public PiiAccessAudit toDomain(PiiAccessAuditJpaEntity entity) {
        if (entity == null) return null;

        PiiAccessAudit domain = new PiiAccessAudit();
        domain.setAuditId(entity.getAuditId());
        domain.setPiiIndividualId(entity.getPiiIndividualId());
        domain.setPiiBusinessId(entity.getPiiBusinessId());
        domain.setGlobalUid(entity.getGlobalUid());
        domain.setAccessorId(entity.getAccessorId());
        domain.setAccessorRole(entity.getAccessorRole());
        domain.setAccessorIp(entity.getAccessorIp());
        domain.setAccessedFields(fromPostgresArray(entity.getAccessedFields()));
        domain.setAccessOperation(entity.getAccessOperation());
        domain.setAccessPurpose(toDomainAccessPurpose(entity.getAccessPurpose()));
        domain.setAccessJustification(entity.getAccessJustification());
        domain.setAccessGranted(entity.isAccessGranted());
        domain.setDenialReason(entity.getDenialReason());
        domain.setAccessedAt(toInstant(entity.getAccessedAt()));
        return domain;
    }

    // -----------------------------------------------------------------------
    // Enum conversions
    // -----------------------------------------------------------------------

    private PiiIndividualJpaEntity.VaultRegionEnum toEntityVaultRegion(VaultRegion domainRegion) {
        if (domainRegion == null) return null;
        return PiiIndividualJpaEntity.VaultRegionEnum.valueOf(domainRegion.name());
    }

    private VaultRegion toDomainVaultRegion(PiiIndividualJpaEntity.VaultRegionEnum entityRegion) {
        if (entityRegion == null) return null;
        return VaultRegion.valueOf(entityRegion.name());
    }

    private PiiAccessAuditJpaEntity.AccessPurposeEnum toEntityAccessPurpose(AccessPurpose domainPurpose) {
        if (domainPurpose == null) return null;
        return PiiAccessAuditJpaEntity.AccessPurposeEnum.valueOf(domainPurpose.name());
    }

    private AccessPurpose toDomainAccessPurpose(PiiAccessAuditJpaEntity.AccessPurposeEnum entityPurpose) {
        if (entityPurpose == null) return null;
        return AccessPurpose.valueOf(entityPurpose.name());
    }

    // -----------------------------------------------------------------------
    // Timestamp conversions
    // -----------------------------------------------------------------------

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        if (instant == null) return null;
        return instant.atOffset(ZoneOffset.UTC);
    }

    private Instant toInstant(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.toInstant();
    }

    // -----------------------------------------------------------------------
    // PostgreSQL array conversions
    // -----------------------------------------------------------------------

    private String toPostgresArray(List<String> fields) {
        if (fields == null || fields.isEmpty()) return "{}";
        return "{" + String.join(",", fields) + "}";
    }

    private List<String> fromPostgresArray(String pgArray) {
        if (pgArray == null || pgArray.isEmpty() || "{}".equals(pgArray)) {
            return List.of();
        }
        String content = pgArray.replaceAll("[{}]", "");
        return Arrays.stream(content.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
