package com.ksa.financing.identity.infrastructure.persistence.mapper;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.infrastructure.persistence.entity.UserIdentityJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class UserIdentityPersistenceMapper {

    public UserIdentityJpaEntity toEntity(UserIdentity domain) {
        if (domain == null) {
            return null;
        }
        UserIdentityJpaEntity entity = new UserIdentityJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setKeycloakUserId(domain.getKeycloakUserId());
        entity.setKeycloakRealm(domain.getKeycloakRealm());
        entity.setKeycloakUsername(domain.getKeycloakUsername());
        entity.setInternalUserId(domain.getInternalUserId());
        entity.setInternalCustomerId(domain.getInternalCustomerId());
        entity.setInternalPartnerId(domain.getInternalPartnerId());
        entity.setGlobalUid(domain.getGlobalUid());
        entity.setMobileNumber(domain.getMobileNumber());
        entity.setUserType(domain.getUserType());
        entity.setStatus(domain.getStatus());
        entity.setResetOtp(domain.getResetOtp());
        entity.setResetOtpExpiry(toOffsetDateTime(domain.getResetOtpExpiry()));
        entity.setLastSyncedAt(toOffsetDateTime(domain.getLastSyncedAt()));
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public UserIdentity toDomain(UserIdentityJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        UserIdentity domain = new UserIdentity();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setKeycloakUserId(entity.getKeycloakUserId());
        domain.setKeycloakRealm(entity.getKeycloakRealm());
        domain.setKeycloakUsername(entity.getKeycloakUsername());
        domain.setInternalUserId(entity.getInternalUserId());
        domain.setInternalCustomerId(entity.getInternalCustomerId());
        domain.setInternalPartnerId(entity.getInternalPartnerId());
        domain.setGlobalUid(entity.getGlobalUid());
        domain.setMobileNumber(entity.getMobileNumber());
        domain.setUserType(entity.getUserType());
        domain.setStatus(entity.getStatus());
        domain.setResetOtp(entity.getResetOtp());
        domain.setResetOtpExpiry(toInstant(entity.getResetOtpExpiry()));
        domain.setLastSyncedAt(toInstant(entity.getLastSyncedAt()));
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }
}
