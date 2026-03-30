package com.ksa.financing.kycadapter.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import com.ksa.financing.kycadapter.domain.model.KycProvider;
import com.ksa.financing.kycadapter.domain.port.out.ProviderCacheRepository;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.ProviderResponseCacheJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity.KycProviderEnum;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity.VerificationResultEnum;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity.VerificationTypeEnum;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing the domain ProviderCacheRepository port.
 * Provides caching of provider API responses with TTL-based expiry.
 */
@Repository
@RequiredArgsConstructor
public class ProviderCacheRepositoryImpl implements ProviderCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(ProviderCacheRepositoryImpl.class);

    private final JpaProviderCacheRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<Map<String, Object>> findCachedResponse(String cacheKey) {
        log.debug("Looking up cached response for cacheKey={}", cacheKey);

        return jpaRepository.findByCacheKeyAndTenantId(cacheKey, null)
                .filter(entity -> !entity.isStale())
                .filter(entity -> entity.getValidUntil().isAfter(OffsetDateTime.now()))
                .map(entity -> {
                    // Increment hit count
                    entity.setHitCount(entity.getHitCount() + 1);
                    entity.setLastHitAt(OffsetDateTime.now());
                    jpaRepository.save(entity);

                    return deserializePayload(entity.getResponsePayload());
                });
    }

    @Override
    public void cacheResponse(String cacheKey, KycProvider provider, String subjectId,
                              Map<String, Object> response, int ttlHours) {
        log.debug("Caching response for cacheKey={}, provider={}, ttlHours={}", cacheKey, provider, ttlHours);

        // Upsert: find existing entry or create new one to avoid duplicate key violations on retry
        ProviderResponseCacheJpaEntity entity = jpaRepository.findByCacheKeyAndTenantId(cacheKey, null)
                .orElseGet(ProviderResponseCacheJpaEntity::new);

        entity.setCacheKey(cacheKey);
        entity.setProvider(KycProviderEnum.valueOf(provider.name()));
        entity.setVerificationType(VerificationTypeEnum.NATIONAL_ID);
        entity.setSubjectId(subjectId);
        entity.setSubjectIdType("NATIONAL_ID");
        entity.setRequestHash(computeHash(cacheKey));
        entity.setResponsePayload(serializePayload(response));
        entity.setResult(VerificationResultEnum.VERIFIED);
        entity.setFetchedAt(OffsetDateTime.now());
        entity.setValidUntil(OffsetDateTime.now().plusHours(ttlHours));
        entity.setStale(false);
        entity.setHitCount(0);

        jpaRepository.save(entity);
        log.debug("Cached response with id={}", entity.getId());
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cache payload", e);
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to serialize cache payload", e);
        }
    }

    private Map<String, Object> deserializePayload(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cache payload", e);
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to deserialize cache payload", e);
        }
    }

    @Override
    public Optional<Map<String, Object>> findCachedResponse(UUID tenantId, String cacheKey) {
        log.debug("Looking up cached response for tenantId={}, cacheKey={}", tenantId, cacheKey);

        return jpaRepository.findByCacheKeyAndTenantId(cacheKey, tenantId)
                .filter(entity -> !entity.isStale())
                .filter(entity -> entity.getValidUntil().isAfter(OffsetDateTime.now()))
                .map(entity -> {
                    entity.setHitCount(entity.getHitCount() + 1);
                    entity.setLastHitAt(OffsetDateTime.now());
                    jpaRepository.save(entity);

                    return deserializePayload(entity.getResponsePayload());
                });
    }

    @Override
    public void cacheResponse(UUID tenantId, String cacheKey, KycProvider provider, String subjectId,
                              Map<String, Object> response, int ttlHours) {
        log.debug("Caching response for tenantId={}, cacheKey={}, provider={}, ttlHours={}",
                tenantId, cacheKey, provider, ttlHours);

        // Upsert: find existing entry or create new one to avoid duplicate key violations on retry
        ProviderResponseCacheJpaEntity entity = jpaRepository.findByCacheKeyAndTenantId(cacheKey, tenantId)
                .orElseGet(ProviderResponseCacheJpaEntity::new);

        entity.setTenantId(tenantId);
        entity.setCacheKey(cacheKey);
        entity.setProvider(KycProviderEnum.valueOf(provider.name()));
        entity.setVerificationType(VerificationTypeEnum.NATIONAL_ID);
        entity.setSubjectId(subjectId);
        entity.setSubjectIdType("NATIONAL_ID");
        entity.setRequestHash(computeHash(cacheKey));
        entity.setResponsePayload(serializePayload(response));
        entity.setResult(VerificationResultEnum.VERIFIED);
        entity.setFetchedAt(OffsetDateTime.now());
        entity.setValidUntil(OffsetDateTime.now().plusHours(ttlHours));
        entity.setStale(false);
        entity.setHitCount(0);

        jpaRepository.save(entity);
        log.debug("Cached response with id={}", entity.getId());
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "SHA-256 algorithm not available", e);
        }
    }
}
