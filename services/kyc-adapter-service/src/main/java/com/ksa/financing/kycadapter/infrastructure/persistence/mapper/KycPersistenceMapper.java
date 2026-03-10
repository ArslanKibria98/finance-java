package com.ksa.financing.kycadapter.infrastructure.persistence.mapper;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.kycadapter.domain.model.*;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Mapper between domain model VerificationSession and JPA entity VerificationSessionJpaEntity.
 * Handles conversion between domain enums and JPA entity enums, and between
 * java.time.Instant (domain) and java.time.OffsetDateTime (persistence).
 */
@Component
public class KycPersistenceMapper {

    /**
     * Convert a JPA entity to a domain model.
     */
    public VerificationSession toDomain(VerificationSessionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        VerificationSession session = new VerificationSession();
        session.setId(entity.getId());
        session.setTenantId(entity.getTenantId());
        session.setSessionNumber(entity.getSessionNumber());
        session.setCustomerId(entity.getCustomerId());
        session.setGlobalUid(entity.getGlobalUid());
        session.setLoanApplicationId(entity.getLoanApplicationId());
        session.setCountryCode(entity.getCountryCode());
        session.setVerificationType(toVerificationType(entity.getVerificationType()));
        session.setProvider(toKycProvider(entity.getProvider()));
        session.setNationalId(entity.getNationalId() != null ? NationalId.of(entity.getNationalId()) : null);
        session.setIqamaNumber(entity.getIqamaNumber());
        session.setCommercialRegistration(entity.getCommercialRegistration());
        session.setDateOfBirth(entity.getDateOfBirth());
        session.setFullNameAr(entity.getFullNameAr());
        session.setFullNameEn(entity.getFullNameEn());
        session.setStatus(toSessionStatus(entity.getStatus()));
        session.setProviderSessionId(entity.getProviderSessionId());
        session.setProviderRequestId(entity.getProviderRequestId());
        session.setResult(toVerificationResult(entity.getResult()));
        session.setConfidenceScore(entity.getConfidenceScore());
        session.setInitiatedAt(toInstant(entity.getInitiatedAt()));
        session.setUserActionAt(toInstant(entity.getUserActionAt()));
        session.setCompletedAt(toInstant(entity.getCompletedAt()));
        session.setExpiresAt(toInstant(entity.getExpiresAt()));
        session.setAttemptCount(entity.getAttemptCount());
        session.setMaxAttempts(entity.getMaxAttempts());
        session.setIdempotencyKey(entity.getIdempotencyKey());
        session.setCreatedAt(toInstant(entity.getCreatedAt()));
        session.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        session.setVersion(entity.getVersion());

        return session;
    }

    /**
     * Convert a domain model to a JPA entity.
     */
    public VerificationSessionJpaEntity toEntity(VerificationSession session) {
        if (session == null) {
            return null;
        }

        VerificationSessionJpaEntity entity = new VerificationSessionJpaEntity();
        entity.setId(session.getId());
        entity.setTenantId(session.getTenantId());
        entity.setSessionNumber(session.getSessionNumber());
        entity.setCustomerId(session.getCustomerId());
        entity.setGlobalUid(session.getGlobalUid());
        entity.setLoanApplicationId(session.getLoanApplicationId());
        entity.setCountryCode(session.getCountryCode() != null ? session.getCountryCode() : "SAU");
        entity.setVerificationType(toVerificationTypeEnum(session.getVerificationType()));
        entity.setProvider(toKycProviderEnum(session.getProvider()));
        entity.setNationalId(session.getNationalId() != null ? session.getNationalId().value() : null);
        entity.setIqamaNumber(session.getIqamaNumber());
        entity.setCommercialRegistration(session.getCommercialRegistration());
        entity.setDateOfBirth(session.getDateOfBirth());
        entity.setFullNameAr(session.getFullNameAr());
        entity.setFullNameEn(session.getFullNameEn());
        entity.setStatus(toSessionStatusEnum(session.getStatus()));
        entity.setProviderSessionId(session.getProviderSessionId());
        entity.setProviderRequestId(session.getProviderRequestId());
        entity.setResult(toVerificationResultEnum(session.getResult()));
        entity.setConfidenceScore(session.getConfidenceScore());
        entity.setInitiatedAt(toOffsetDateTime(session.getInitiatedAt()));
        entity.setUserActionAt(toOffsetDateTime(session.getUserActionAt()));
        entity.setCompletedAt(toOffsetDateTime(session.getCompletedAt()));
        entity.setExpiresAt(toOffsetDateTime(session.getExpiresAt()));
        entity.setAttemptCount(session.getAttemptCount());
        entity.setMaxAttempts(session.getMaxAttempts());
        entity.setIdempotencyKey(session.getIdempotencyKey());
        entity.setCreatedAt(toOffsetDateTime(session.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(session.getUpdatedAt()));
        entity.setVersion(session.getVersion());

        return entity;
    }

    // -- Enum conversions: domain <-> entity --

    private VerificationType toVerificationType(VerificationTypeEnum e) {
        return e != null ? VerificationType.valueOf(e.name()) : null;
    }

    private VerificationTypeEnum toVerificationTypeEnum(VerificationType t) {
        return t != null ? VerificationTypeEnum.valueOf(t.name()) : null;
    }

    private KycProvider toKycProvider(KycProviderEnum e) {
        return e != null ? KycProvider.valueOf(e.name()) : null;
    }

    private KycProviderEnum toKycProviderEnum(KycProvider p) {
        return p != null ? KycProviderEnum.valueOf(p.name()) : null;
    }

    private SessionStatus toSessionStatus(SessionStatusEnum e) {
        return e != null ? SessionStatus.valueOf(e.name()) : null;
    }

    private SessionStatusEnum toSessionStatusEnum(SessionStatus s) {
        return s != null ? SessionStatusEnum.valueOf(s.name()) : null;
    }

    private VerificationResult toVerificationResult(VerificationResultEnum e) {
        return e != null ? VerificationResult.valueOf(e.name()) : null;
    }

    private VerificationResultEnum toVerificationResultEnum(VerificationResult r) {
        return r != null ? VerificationResultEnum.valueOf(r.name()) : null;
    }

    // -- Timestamp conversions: Instant <-> OffsetDateTime --

    private Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
