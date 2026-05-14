package com.ksa.financing.ledger.infrastructure.fineract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.ledger.infrastructure.persistence.entity.FineractProxyIdempotencyJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaFineractProxyIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotency cache for Fineract proxy calls.
 * Same {@code Idempotency-Key} on the same operation with the same payload returns the cached response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FineractIdempotencyService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JpaFineractProxyIdempotencyRepository repository;
    private final ObjectMapper objectMapper;
    private final FineractProxyProperties props;

    @Transactional(readOnly = true)
    public Optional<CachedResponse> lookup(UUID tenantId, String idempotencyKey, String operation, Map<String, Object> request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        String requestHash = hash(request);
        return repository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .filter(row -> row.getExpiresAt().isAfter(OffsetDateTime.now()))
                .map(row -> {
                    if (!row.getOperation().equals(operation) || !row.getRequestHash().equals(requestHash)) {
                        log.warn("Idempotency-Key={} reused with different request body or operation (tenant={})",
                                idempotencyKey, tenantId);
                        return new CachedResponse(409,
                                Map.of("error", "Idempotency-Key reused with different payload"), true);
                    }
                    return new CachedResponse(row.getCachedStatus(), row.getCachedResponse(), false);
                });
    }

    @Transactional
    public void store(UUID tenantId, String idempotencyKey, String operation,
                      Map<String, Object> request, JsonNode response, int status) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        if (status < 200 || status >= 300) return; // only cache successful responses
        try {
            FineractProxyIdempotencyJpaEntity row = FineractProxyIdempotencyJpaEntity.builder()
                    .tenantId(tenantId)
                    .idempotencyKey(idempotencyKey)
                    .operation(operation)
                    .requestHash(hash(request))
                    .cachedResponse(toMap(response))
                    .cachedStatus(status)
                    .expiresAt(OffsetDateTime.now().plusHours(props.getIdempotencyTtlHours()))
                    .build();
            repository.save(row);
        } catch (Exception e) {
            log.warn("Failed to cache idempotency entry tenant={} key={}: {}",
                    tenantId, idempotencyKey, e.getMessage());
        }
    }

    @Scheduled(cron = "0 15 * * * *") // hourly at :15
    @Transactional
    public void purgeExpired() {
        int deleted = repository.deleteExpired(OffsetDateTime.now());
        if (deleted > 0) log.info("Purged {} expired Fineract proxy idempotency rows", deleted);
    }

    private String hash(Map<String, Object> request) {
        try {
            String body = request == null ? "" : objectMapper.writeValueAsString(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return "0";
        }
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isNull()) return Map.of();
        try {
            return objectMapper.convertValue(node, MAP_TYPE);
        } catch (Exception e) {
            return Map.of("raw", node.toString());
        }
    }

    public record CachedResponse(int status, Map<String, Object> body, boolean conflict) {}
}
