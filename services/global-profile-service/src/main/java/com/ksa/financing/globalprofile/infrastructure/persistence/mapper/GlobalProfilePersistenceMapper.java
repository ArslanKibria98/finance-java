package com.ksa.financing.globalprofile.infrastructure.persistence.mapper;

import com.ksa.financing.globalprofile.domain.model.CustomerType;
import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.GlobalKycAggregateStatus;
import com.ksa.financing.globalprofile.domain.model.KycStatus;
import com.ksa.financing.globalprofile.domain.model.PiiAccessToken;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import com.ksa.financing.globalprofile.infrastructure.persistence.entity.GlobalCustomerJpaEntity;
import com.ksa.financing.globalprofile.infrastructure.persistence.entity.PiiAccessTokenJpaEntity;
import com.ksa.financing.globalprofile.infrastructure.persistence.entity.RegionalProfileJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between domain models and JPA entities for the Global Profile Service.
 * <p>
 * Handles conversion of:
 * <ul>
 *   <li>Enum types between domain and entity layers</li>
 *   <li>Instant ↔ OffsetDateTime for timestamp columns</li>
 *   <li>List&lt;String&gt; ↔ PostgreSQL array string for allowed_fields</li>
 * </ul>
 */
@Component
public class GlobalProfilePersistenceMapper {

    // -----------------------------------------------------------------------
    // GlobalCustomer ↔ GlobalCustomerJpaEntity
    // -----------------------------------------------------------------------

    public GlobalCustomerJpaEntity toEntity(GlobalCustomer domain) {
        if (domain == null) return null;

        GlobalCustomerJpaEntity entity = new GlobalCustomerJpaEntity();
        entity.setGlobalUid(domain.getGlobalUid());
        entity.setCustomerType(toEntityCustomerType(domain.getCustomerType()));
        entity.setGlobalEmailHash(domain.getGlobalEmailHash());
        entity.setGlobalMobileHash(domain.getGlobalMobileHash());
        entity.setPrimaryCountryCode(domain.getPrimaryCountryCode());
        entity.setGlobalKycStatus(toEntityKycAggregateStatus(domain.getGlobalKycStatus()));
        entity.setKycLastVerifiedAt(toOffsetDateTime(domain.getKycLastVerifiedAt()));
        entity.setGlobalRiskGrade(domain.getGlobalRiskGrade());
        entity.setGlobalRiskUpdatedAt(toOffsetDateTime(domain.getGlobalRiskUpdatedAt()));
        entity.setPepFlag(domain.isPepFlag());
        entity.setSanctionsFlag(domain.isSanctionsFlag());
        entity.setFraudFlag(domain.isFraudFlag());
        entity.setActive(domain.isActive());
        entity.setBlockedAt(toOffsetDateTime(domain.getBlockedAt()));
        entity.setBlockedReason(domain.getBlockedReason());
        entity.setAcquisitionSource(domain.getAcquisitionSource());
        entity.setCustomerSegment(domain.getCustomerSegment());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public GlobalCustomer toDomain(GlobalCustomerJpaEntity entity) {
        if (entity == null) return null;

        GlobalCustomer domain = new GlobalCustomer();
        domain.setGlobalUid(entity.getGlobalUid());
        domain.setCustomerType(toDomainCustomerType(entity.getCustomerType()));
        domain.setGlobalEmailHash(entity.getGlobalEmailHash());
        domain.setGlobalMobileHash(entity.getGlobalMobileHash());
        domain.setPrimaryCountryCode(entity.getPrimaryCountryCode());
        domain.setGlobalKycStatus(toDomainKycAggregateStatus(entity.getGlobalKycStatus()));
        domain.setKycLastVerifiedAt(toInstant(entity.getKycLastVerifiedAt()));
        domain.setGlobalRiskGrade(entity.getGlobalRiskGrade());
        domain.setGlobalRiskUpdatedAt(toInstant(entity.getGlobalRiskUpdatedAt()));
        domain.setPepFlag(entity.isPepFlag());
        domain.setSanctionsFlag(entity.isSanctionsFlag());
        domain.setFraudFlag(entity.isFraudFlag());
        domain.setActive(entity.isActive());
        domain.setBlockedAt(toInstant(entity.getBlockedAt()));
        domain.setBlockedReason(entity.getBlockedReason());
        domain.setAcquisitionSource(entity.getAcquisitionSource());
        domain.setCustomerSegment(entity.getCustomerSegment());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // -----------------------------------------------------------------------
    // RegionalProfile ↔ RegionalProfileJpaEntity
    // -----------------------------------------------------------------------

    public RegionalProfileJpaEntity toEntity(RegionalProfile domain) {
        if (domain == null) return null;

        RegionalProfileJpaEntity entity = new RegionalProfileJpaEntity();
        entity.setRegionalProfileId(domain.getRegionalProfileId());
        entity.setGlobalUid(domain.getGlobalUid());
        entity.setCountryCode(domain.getCountryCode());
        entity.setRegionalCifNumber(domain.getRegionalCifNumber());
        entity.setRegionalKycStatus(toEntityKycStatus(domain.getRegionalKycStatus()));
        entity.setKycVerifiedAt(toOffsetDateTime(domain.getKycVerifiedAt()));
        entity.setKycExpiryDate(domain.getKycExpiryDate());
        entity.setKycProvider(domain.getKycProvider());
        entity.setPiiVaultRegion(domain.getPiiVaultRegion());
        entity.setPiiVaultRecordId(domain.getPiiVaultRecordId());
        entity.setRegionalRiskGrade(domain.getRegionalRiskGrade());
        entity.setRiskGradeUpdatedAt(toOffsetDateTime(domain.getRiskGradeUpdatedAt()));
        entity.setRegionalPepFlag(domain.isRegionalPepFlag());
        entity.setRegionalSanctionsFlag(domain.isRegionalSanctionsFlag());
        entity.setKeycloakUserId(domain.getKeycloakUserId());
        entity.setActive(domain.isActive());
        entity.setActivationDate(domain.getActivationDate());
        entity.setDeactivationDate(domain.getDeactivationDate());
        entity.setDeactivationReason(domain.getDeactivationReason());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public RegionalProfile toDomain(RegionalProfileJpaEntity entity) {
        if (entity == null) return null;

        RegionalProfile domain = new RegionalProfile();
        domain.setRegionalProfileId(entity.getRegionalProfileId());
        domain.setGlobalUid(entity.getGlobalUid());
        domain.setCountryCode(entity.getCountryCode());
        domain.setRegionalCifNumber(entity.getRegionalCifNumber());
        domain.setRegionalKycStatus(toDomainKycStatus(entity.getRegionalKycStatus()));
        domain.setKycVerifiedAt(toInstant(entity.getKycVerifiedAt()));
        domain.setKycExpiryDate(entity.getKycExpiryDate());
        domain.setKycProvider(entity.getKycProvider());
        domain.setPiiVaultRegion(entity.getPiiVaultRegion());
        domain.setPiiVaultRecordId(entity.getPiiVaultRecordId());
        domain.setRegionalRiskGrade(entity.getRegionalRiskGrade());
        domain.setRiskGradeUpdatedAt(toInstant(entity.getRiskGradeUpdatedAt()));
        domain.setRegionalPepFlag(entity.isRegionalPepFlag());
        domain.setRegionalSanctionsFlag(entity.isRegionalSanctionsFlag());
        domain.setKeycloakUserId(entity.getKeycloakUserId());
        domain.setActive(entity.isActive());
        domain.setActivationDate(entity.getActivationDate());
        domain.setDeactivationDate(entity.getDeactivationDate());
        domain.setDeactivationReason(entity.getDeactivationReason());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // -----------------------------------------------------------------------
    // PiiAccessToken ↔ PiiAccessTokenJpaEntity
    // -----------------------------------------------------------------------

    public PiiAccessTokenJpaEntity toEntity(PiiAccessToken domain) {
        if (domain == null) return null;

        PiiAccessTokenJpaEntity entity = new PiiAccessTokenJpaEntity();
        entity.setTokenId(domain.getTokenId());
        entity.setAccessToken(domain.getAccessToken());
        entity.setTokenHash(domain.getTokenHash());
        entity.setGlobalUid(domain.getGlobalUid());
        entity.setRegionalProfileId(domain.getRegionalProfileId());
        entity.setPiiVaultRegion(domain.getPiiVaultRegion());
        entity.setAllowedFields(toPostgresArray(domain.getAllowedFields()));
        entity.setRequesterId(domain.getRequesterId());
        entity.setRequesterRole(domain.getRequesterRole());
        entity.setRequesterIp(domain.getRequesterIp());
        entity.setAccessPurpose(domain.getAccessPurpose());
        entity.setRelatedEntityType(domain.getRelatedEntityType());
        entity.setRelatedEntityId(domain.getRelatedEntityId());
        entity.setIssuedAt(toOffsetDateTime(domain.getIssuedAt()));
        entity.setExpiresAt(toOffsetDateTime(domain.getExpiresAt()));
        entity.setRevoked(domain.isRevoked());
        entity.setRevokedAt(toOffsetDateTime(domain.getRevokedAt()));
        entity.setRevokedReason(domain.getRevokedReason());
        entity.setUsedCount(domain.getUsedCount());
        entity.setLastUsedAt(toOffsetDateTime(domain.getLastUsedAt()));
        return entity;
    }

    public PiiAccessToken toDomain(PiiAccessTokenJpaEntity entity) {
        if (entity == null) return null;

        PiiAccessToken domain = new PiiAccessToken();
        domain.setTokenId(entity.getTokenId());
        domain.setAccessToken(entity.getAccessToken());
        domain.setTokenHash(entity.getTokenHash());
        domain.setGlobalUid(entity.getGlobalUid());
        domain.setRegionalProfileId(entity.getRegionalProfileId());
        domain.setPiiVaultRegion(entity.getPiiVaultRegion());
        domain.setAllowedFields(fromPostgresArray(entity.getAllowedFields()));
        domain.setRequesterId(entity.getRequesterId());
        domain.setRequesterRole(entity.getRequesterRole());
        domain.setRequesterIp(entity.getRequesterIp());
        domain.setAccessPurpose(entity.getAccessPurpose());
        domain.setRelatedEntityType(entity.getRelatedEntityType());
        domain.setRelatedEntityId(entity.getRelatedEntityId());
        domain.setIssuedAt(toInstant(entity.getIssuedAt()));
        domain.setExpiresAt(toInstant(entity.getExpiresAt()));
        domain.setRevoked(entity.isRevoked());
        domain.setRevokedAt(toInstant(entity.getRevokedAt()));
        domain.setRevokedReason(entity.getRevokedReason());
        domain.setUsedCount(entity.getUsedCount());
        domain.setLastUsedAt(toInstant(entity.getLastUsedAt()));
        return domain;
    }

    // -----------------------------------------------------------------------
    // Enum conversions
    // -----------------------------------------------------------------------

    private GlobalCustomerJpaEntity.CustomerTypeEnum toEntityCustomerType(CustomerType domainType) {
        if (domainType == null) return null;
        return GlobalCustomerJpaEntity.CustomerTypeEnum.valueOf(domainType.name());
    }

    private CustomerType toDomainCustomerType(GlobalCustomerJpaEntity.CustomerTypeEnum entityType) {
        if (entityType == null) return null;
        return CustomerType.valueOf(entityType.name());
    }

    private GlobalCustomerJpaEntity.GlobalKycAggregateStatusEnum toEntityKycAggregateStatus(
            GlobalKycAggregateStatus domainStatus) {
        if (domainStatus == null) return null;
        return GlobalCustomerJpaEntity.GlobalKycAggregateStatusEnum.valueOf(domainStatus.name());
    }

    private GlobalKycAggregateStatus toDomainKycAggregateStatus(
            GlobalCustomerJpaEntity.GlobalKycAggregateStatusEnum entityStatus) {
        if (entityStatus == null) return null;
        return GlobalKycAggregateStatus.valueOf(entityStatus.name());
    }

    private RegionalProfileJpaEntity.KycStatusEnum toEntityKycStatus(KycStatus domainStatus) {
        if (domainStatus == null) return null;
        return RegionalProfileJpaEntity.KycStatusEnum.valueOf(domainStatus.name());
    }

    private KycStatus toDomainKycStatus(RegionalProfileJpaEntity.KycStatusEnum entityStatus) {
        if (entityStatus == null) return null;
        return KycStatus.valueOf(entityStatus.name());
    }

    // -----------------------------------------------------------------------
    // Timestamp conversions (Instant ↔ OffsetDateTime)
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
    // PostgreSQL array conversions (List<String> ↔ String)
    // -----------------------------------------------------------------------

    /**
     * Converts a list of strings to a PostgreSQL array literal.
     * Example: ["email", "mobile"] → "{email,mobile}"
     */
    private String toPostgresArray(List<String> fields) {
        if (fields == null || fields.isEmpty()) return "{}";
        return "{" + String.join(",", fields) + "}";
    }

    /**
     * Converts a PostgreSQL array literal back to a list of strings.
     * Example: "{email,mobile}" → ["email", "mobile"]
     */
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
