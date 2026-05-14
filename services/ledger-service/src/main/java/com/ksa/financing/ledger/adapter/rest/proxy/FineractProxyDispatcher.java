package com.ksa.financing.ledger.adapter.rest.proxy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.ledger.infrastructure.fineract.FineractAuditLogService;
import com.ksa.financing.ledger.infrastructure.fineract.FineractAuditLogService.AuditEntry;
import com.ksa.financing.ledger.infrastructure.fineract.FineractGateway;
import com.ksa.financing.ledger.infrastructure.fineract.FineractGateway.FineractCallResult;
import com.ksa.financing.ledger.infrastructure.fineract.FineractIdempotencyService;
import com.ksa.financing.ledger.infrastructure.fineract.FineractIdempotencyService.CachedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Shared dispatcher used by every Fineract proxy controller.
 * Centralises tenant extraction, idempotency, audit logging, and Fineract dispatch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FineractProxyDispatcher {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final FineractGateway gateway;
    private final FineractAuditLogService auditLog;
    private final FineractIdempotencyService idempotency;
    private final ObjectMapper objectMapper;

    public ResponseEntity<Map<String, Object>> dispatchPost(
            Jwt jwt, String callerService, String correlationId, String idempotencyKey,
            String operation, String relativePath, Map<String, Object> body) {
        return dispatch(jwt, callerService, correlationId, idempotencyKey, operation,
                HttpMethod.POST, relativePath, body);
    }

    public ResponseEntity<Map<String, Object>> dispatchGet(
            Jwt jwt, String callerService, String correlationId, String idempotencyKey,
            String operation, String relativePath) {
        return dispatch(jwt, callerService, correlationId, idempotencyKey, operation,
                HttpMethod.GET, relativePath, null);
    }

    public ResponseEntity<Map<String, Object>> dispatchDelete(
            Jwt jwt, String callerService, String correlationId,
            String operation, String relativePath) {
        return dispatch(jwt, callerService, correlationId, null, operation,
                HttpMethod.DELETE, relativePath, null);
    }

    /**
     * Internal-call variant: no JWT, tenant is taken from header.
     */
    public ResponseEntity<Map<String, Object>> dispatchInternal(
            UUID tenantId, String callerService, String correlationId, String idempotencyKey,
            String operation, HttpMethod method, String relativePath, Map<String, Object> body) {
        return dispatchInternal(tenantId, callerService, null, correlationId, idempotencyKey,
                operation, method, relativePath, body);
    }

    public ResponseEntity<Map<String, Object>> dispatchInternal(
            UUID tenantId, String callerService, String callerUserId,
            String correlationId, String idempotencyKey,
            String operation, HttpMethod method, String relativePath, Map<String, Object> body) {

        return execute(tenantId, callerService, callerUserId, correlationId, idempotencyKey,
                operation, method, relativePath, body);
    }

    private ResponseEntity<Map<String, Object>> dispatch(
            Jwt jwt, String callerService, String correlationId, String idempotencyKey,
            String operation, HttpMethod method, String relativePath, Map<String, Object> body) {

        UUID tenantId = extractTenantId(jwt);
        String callerUserId = jwt != null ? jwt.getSubject() : null;
        String resolvedCaller = callerService != null ? callerService : "unknown";

        return execute(tenantId, resolvedCaller, callerUserId, correlationId, idempotencyKey,
                operation, method, relativePath, body);
    }

    private ResponseEntity<Map<String, Object>> execute(
            UUID tenantId, String callerService, String callerUserId,
            String correlationId, String idempotencyKey, String operation,
            HttpMethod method, String relativePath, Map<String, Object> body) {

        // Idempotency lookup (only meaningful for state-changing ops)
        if (idempotencyKey != null && method != HttpMethod.GET) {
            CachedResponse cached = idempotency.lookup(tenantId, idempotencyKey, operation, body).orElse(null);
            if (cached != null) {
                if (cached.conflict()) {
                    return ResponseEntity.status(409).body(cached.body());
                }
                log.info("Idempotency hit op={} key={} tenant={}", operation, idempotencyKey, tenantId);
                return ResponseEntity.status(cached.status()).body(cached.body());
            }
        }

        FineractCallResult result = gateway.call(method, relativePath, body);

        Map<String, Object> responseMap = result.body() != null
                ? objectMapper.convertValue(result.body(), MAP_TYPE)
                : (result.errorMessage() != null
                    ? Map.of("error", result.errorMessage())
                    : Map.of());

        auditLog.record(new AuditEntry(
                tenantId, callerService, callerUserId, operation, relativePath, method.name(),
                body, result.body(), result.status(), idempotencyKey, correlationId,
                result.errorMessage(), result.durationMs()));

        if (result.isSuccess() && idempotencyKey != null && method != HttpMethod.GET) {
            idempotency.store(tenantId, idempotencyKey, operation, body, result.body(), result.status());
        }

        return ResponseEntity.status(result.status()).body(responseMap);
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantIdStr = jwt != null ? jwt.getClaimAsString("tenant_id") : null;
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantIdStr);
    }
}
